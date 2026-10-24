# generic-object-management

Thư viện Spring Boot (package `com.msm.core.objects`) cung cấp sẵn cho các service MSM:

- **REST API generic** cho mọi object: filter, lookup, CRUD, bulk, named action, query, conversion.
- **CRUD engine** trên jOOQ, có kiểm tra quyền, xóa mềm, audit, khóa lạc quan, `customValues`.
- **Import CSV/Excel** (qua S3 presigned URL, staging, báo lỗi theo dòng) và **export Excel**.
- **Client gọi service nội bộ** và **client tích hợp hệ thống ngoài** (auth, retry, ghi log tích hợp).

Thư viện xây trên [commons](https://github.com/msm-lib/commons). Các khái niệm metadata, `TypedAttribute`, filter, action/hook được mô tả trong README của commons.

## Cài đặt

Bản phát hành là **git tag** (ví dụ `2.5.3`), build qua [JitPack](https://jitpack.io/#msm-lib/generic-object-management).

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.msm-lib:generic-object-management:2.5.3")
    // Thêm dòng này nếu service dùng trực tiếp type của commons. Phải cùng version với msmCommonVersion của thư viện này
    implementation("com.github.msm-lib:commons:2.3.7")
}
```

Yêu cầu:

- Java 21 và Spring Boot 3.5.x.
- PostgreSQL, có extension `unaccent`.
- Redis, nếu dùng OAuth token cho tích hợp.
- S3 hoặc dịch vụ tương thích S3, nếu dùng import/export.

## Tích hợp vào service

Thư viện tự cấu hình qua `MsmAutoConfiguration`, được nạp bằng `META-INF/spring/...AutoConfiguration.imports`. Service cần làm thêm các việc sau.

### 1. Bật các annotation

```java
@SpringBootApplication
@EnableMethodSecurity   // thiếu: @PreAuthorize trên controller không có tác dụng
@EnableAsync            // thiếu: export chạy đồng bộ ngay trong request
public class OrderServiceApplication { }
```

### 2. Cung cấp bean

Thư viện không tự tạo các bean sau:

- `DSLContext` (jOOQ)
- `EntityManager` (JPA)
- `RestClient.Builder`
- `ObjectMapper`
- `S3Presigner`, bắt buộc cho import/export
- `LettuceConnectionFactory` và `StringRedisTemplate` cho Redis

`S3Client` là tùy chọn; nếu không khai báo, thư viện dùng `S3Client.create()`.

Thư viện **không** gắn header auth hay tenant vào request gọi service khác. Nếu API nội bộ cần các header này, hãy cấu hình interceptor trên `RestClient.Builder`.

### 3. Cấu hình

```yaml
objects:
  executor: { core: 5, max: 20 }
  api-config:
    base-url: https://api.example.com      # bắt buộc khi gọi sang service khác (service không có trong service-urls)
    service-urls:                          # tùy chọn: host nội bộ theo service, lib nối thêm "/<service>"
      user: ${USER_SERVICE_URL:}           # trống (biến không set) thì dùng base-url
      master-data: ${MASTER_DATA_SERVICE_URL:}

aws:
  s3:
    bucket-name: my-bucket
    region: ap-southeast-1
    endpoint:                               # tùy chọn (MinIO, LocalStack...)
    sub-folder: order                       # nên đặt, nếu để trống đường dẫn file sẽ thiếu phần object/ngày
    presigned-url-expiration: 15            # phút

integration:
  connectors:
    my-partner:
      base-url: https://partner.example.com
      auth: { ... }                         # api-key | basic | bearer | oauth2 client-credentials | oauth2 password
      retry: { ... }
```

- Cấu hình import/export (`imports.*`, `exports.*`) có thể đặt trong `classpath:imports/*.yml` và `classpath:exports/*.yml`. Các file này được nạp tự động với **độ ưu tiên cao nhất**, ghi đè cả `application.yml`.
- Nên khai báo `imports.processing` và `exports.processing` ở cấp global.

### 4. Database

Thư viện thêm các JPA entity của nó vào persistence unit của service. Database phải có các bảng:

- `import_job`
- `import_staging`
- `import_error`
- `export_job`
- `data_job`
- `integration_log`
- `object_metadata`, nếu dùng metadata động

Các bảng job kế thừa `SoftDeleteEntity` → `BusinessProcessEntity` → `AuditingEntity`, nên phải có đủ các cột sau:

| Nhóm | Cột |
|---|---|
| Audit | `created_*`, `updated_*` |
| Business process | `team_id`, `org_id`, `workflow_instance_id`, `record_type_id`, `stage_ids`, `approval_status`, `locked_edit` |
| Xóa mềm | `deleted_*`, `is_deleted` |

Nếu thiếu và service đặt `ddl-auto=validate`, ứng dụng sẽ không khởi động được.

### 5. Ghi đè bean

Hầu hết bean có `@ConditionalOnMissingBean`, nên chỉ cần khai báo bean cùng kiểu (hoặc cùng tên) để thay thế. Các bean sau **không** có điều kiện này nên không thay được:

- Hai controller
- `GenericObjectMetadataService`
- `PermissionService`
- `SecurityCheckProvider`
- Các auth provider
- Phần lớn bean import/export

## REST API

Endpoint portal có prefix `/api/v1/cn/portal/generic/objects`. Phiên bản nội bộ có cùng các endpoint, prefix `/api/v1/internal`.

| Method | Path | Mô tả |
|---|---|---|
| POST | `/{obj}/filter` | Filter, phân trang (body `ObjectFilterRequest`) |
| POST | `/{obj}/lookup` | Lookup (dùng quyền LOOKUP) |
| GET | `/{obj}/{id}?returnFields=` | Lấy theo id |
| GET | `/{obj}` | Lấy tất cả |
| POST | `/{obj}` | Tạo mới |
| POST | `/{obj}/bulk` | Tạo nhiều bản ghi |
| PUT | `/{obj}/{id}` | Cập nhật |
| PUT | `/{obj}/bulk` | Cập nhật nhiều bản ghi |
| DELETE | `/{obj}/{id}?version=` | Xóa |
| DELETE | `/{obj}/bulk` | Xóa nhiều bản ghi (body `List<ObjectDeleteRequest>`) |
| PUT | `/{obj}/{id}/{actionName}` | Chạy named action |
| PUT | `/{obj}/bulk/{actionName}` | Chạy named action cho nhiều bản ghi |
| POST | `/query` | Named query (`QueryTemplate`) |
| POST | `/conversion` | Chuyển đổi object |
| POST | `/metadata` | Sinh metadata từ `information_schema` vào `object_metadata` |
| | `/{obj}/imports/...`, `/importjob`, `/exportjob` | Import/export (xem bên dưới) |

- Response trả thẳng `Map`, `List` hoặc `PageResponse`, không có wrapper.
- **API nội bộ không kiểm tra quyền**, nên chỉ được mở trong mạng nội bộ. Các endpoint `/query`, `/metadata` và import cũng chưa có `@PreAuthorize`.

## Tùy biến nghiệp vụ

Mỗi request được chuyển thành `ActionContext(resource = objectName, action = ...)` rồi chạy qua `ActionExecutor`. Nếu object không có handler riêng, `GenericObjectHandler` (resource `*`) xử lý.

Khi khởi động, `ObjectBeanConfigInitializing` **tự scan mọi bean** để đăng ký handler và hook. Vì vậy chỉ cần khai báo Spring bean:

```java
@Component
public class OrderHandlers {

    // Thay logic tạo mới cho object "order"
    @CreateHandler(resource = "order")
    public Map<String, Object> create(ActionContext<Map<String, Object>> ctx) { ... }

    // Named action: PUT /generic/objects/order/{id}/approve
    @Handler(resource = "order", action = "approve")
    @SecuredPermissionAction(anyOf = ...)
    public Map<String, Object> approve(ActionContext<Map<String, Object>> ctx) { ... }

    // Named query: POST /generic/objects/query với query = "orderSummary"
    @Handler(resource = "query-*", action = "orderSummary")
    public Object orderSummary(ActionContext<?> ctx) { ... }

    // Hook chạy trước khi tạo
    @HookBefore(resource = "order", action = Constants.Action.CREATE)
    public void beforeCreate(ActionContext<Map<String, Object>> ctx) { ... }
}
```

- **Key handler:** viết thường. Có nhiều handler cùng khớp một key thì ném lỗi khi chạy.
- **Named action không có `@SecuredPermissionAction`:** mặc định **được phép** cho mọi user.
- **Strategy theo object:**
  - `AuditStrategy`: cách điền field audit.
  - `CustomValueMappingStrategy`: cách đóng gói và trải phẳng `customValues`.

  Mỗi strategy có `support()` trả về tên object; strategy mặc định dùng key `"default"`.
- **Validate có sẵn:** `SystemHookEvent` validate dữ liệu khi create và update (chạy sớm nhất, `order = Integer.MIN_VALUE`). Lỗi được ném dưới dạng `ObjectValidateException` kèm danh sách `ObjectErrorDetail`.

## Khai báo metadata (`*Meta`)

```java
public final class ImportJobMeta {
    private ImportJobMeta() {}

    public static final String OBJECT_NAME = "importjob";                 // viết thường, liền nhau
    public static final Table<?> TABLE = DSL.table(DSL.name("import_job"));

    public static final TypedAttribute<UUID> ID = attr(TABLE, "id", "id", UUID.class);
    public static final TypedAttribute<String> STATUS = attr(TABLE, "status", "status", String.class);
    public static final TypedAttribute<Map<String, Object>> FILE_INFO =
            attr(TABLE, "fileInfo", "file_info", resolve(Map.class, String.class, Object.class));
}
```

- **Cột trong bảng:** `attr(TABLE, "camelField", "snake_column", ...)`. Cột JSONB dùng `resolve(Map.class, String.class, Object.class)`.
- **DTO hoặc nội dung JSON:** `attr("field", ...)`, không truyền `TABLE`. Ví dụ: `S3FileInfoMeta`, `ImportJobDtoMeta`.
- **Code mới:** không dùng `JSONB.class` hay `new TypedAttribute<>(...)` trực tiếp như code cũ.
- **Meta chỉ là lớp xem dữ liệu có kiểu.** Schema thật lúc chạy là `ObjectMetadata`, lấy từ JPA entity hoặc bảng `object_metadata`. Tên field trong Meta, entity và metadata phải khớp nhau.
- **Tên object** phải giống nhau ở mọi nơi: `OBJECT_NAME`, `resource` của handler, `imports.objects.<key>` và `ObjectServiceFactory`.

## Quy tắc CRUD

- **Repository:**
  - `objectQueryRepository` lọc theo data scope của user.
  - `internalObjectQueryRepository` **không** lọc. Code import/export dùng repository này.
- **Thứ tự xử lý khi ghi:** audit (`AuditStrategy`), rồi đóng gói `customValues`, rồi insert/update. Khi đọc: trải phẳng `customValues` ra map gốc.
- **Update:** gộp dữ liệu mới vào bản ghi hiện có rồi **ghi đè cả dòng**, không phải partial patch. Nếu object có `version`, payload phải gửi kèm `version`.
- **Giá trị mặc định và chuẩn hóa text** (populate) chỉ chạy khi **create**.
- **Delete:** xóa mềm nếu metadata có `isDeleted`/`deletedAt`/`deletedBy`/`deletedById`, nếu không thì xóa thật.
- **`code` bị thiếu khi create:** tự sinh ngẫu nhiên. Nếu object có trong `ObjectConstants.PREFIX_OBJECT_CODE` thì sinh theo tiền tố.
- **Nguồn metadata:** `ObjectMetadataFactory` (từ JPA) trước, sau đó đến bảng `object_metadata` (được cache).
- **Người dùng và tenant:** luôn lấy từ `RequestContextHolder` của commons.

## Import / Export

### Import

Trạng thái: `UPLOADING → VALIDATING → VALIDATED | VALIDATION_FAILED → IMPORTING → COMPLETED | COMPLETED_WITH_ERRORS | FAILED`.

1. Gọi `POST /generic/objects/importjob` với body `{ objectName, service, fileName, originalFileName, mimeType, contentLength }`. API trả về job kèm `uploadUrl`.
2. Client upload file thẳng lên S3 bằng `uploadUrl` (presigned PUT).
3. Gọi `POST /generic/objects/{obj}/imports/{excel|csv}/{importJobId}`. API này **validate rồi import luôn**:
   - Validate: dữ liệu được đọc theo lô `imports.processing.batch-size` (mặc định 100). Dòng hợp lệ vào `import_staging`, dòng lỗi vào `import_error`.
   - Có dòng lỗi thì job chuyển sang `VALIDATION_FAILED`, **không import dòng nào**, và API trả về job đó. Lỗi hệ thống khi validate thì job `VALIDATION_FAILED` và API trả lỗi.
   - Validate không lỗi (`VALIDATED`) thì ghi dữ liệu staging vào bảng thật theo từng lô. API trả về job ở trạng thái `COMPLETED`, `COMPLETED_WITH_ERRORS` hoặc lỗi `FAILED`.
   - Chỉ nhận job `UPLOADING`, hoặc `VALIDATED` (đã validate bằng API cũ: chỉ import). Job ở trạng thái khác bị từ chối.
4. `POST /generic/objects/{obj}/imports/{type}/validate` đã **deprecated**; không cần gọi nữa.
5. Tải file lỗi bằng `GET .../{importJobId}/download-errors`, hoặc file gốc bằng `.../download-file`.

Cấu hình cho từng object, đặt trong `imports.objects.<object>`:

```yaml
imports:
  processing: { batch-size: 100 }
  objects:
    category:
      identity: { fields: [code] }
    product:
      source:
        sheet: Products         # tên sheet Excel; bỏ trống thì đọc sheet đầu tiên
        header: { mode: FIXED, row: 0 }   # AUTO | FIXED; bỏ trống thì dùng imports.header
      depends-on: [category]    # object phải được import trước product
      identity:
        fields: [code]          # khóa nhận diện bản ghi
        strategy: UPSERT        # UPSERT | LOOKUP | NONE
      references:
        categoryId:
          lookups:
            - attributeName: code
              sourceField: categoryCode
          fields: [id, code, name]
```

- **`source.sheet`:** áp dụng cho cả bước đọc file Excel lẫn bước ghi cột lỗi vào file gốc. File **chỉ có 1 sheet** thì luôn đọc sheet đó dù tên khác (file mẫu một sheet cũ vẫn dùng được) và không xử lý sheet phụ thuộc. Tên sheet so sánh **không phân biệt hoa thường** (`Contact` khớp sheet `contact`). File nhiều sheet mà không có sheet trùng tên thì ném `ImportConfigurationException`.
- **`imports.objects.<obj>.header`** (cách khai báo cũ) vẫn dùng được, nhưng đã deprecated; hãy chuyển sang `source.header`.
- **`depends-on`:** `ImportConfigService.resolveOrder()` trả về thứ tự import của mọi object đã cấu hình, object phụ thuộc đứng sau các object nó phụ thuộc vào; các object độc lập xếp theo tên. Cấu hình được kiểm tra **khi khởi động**: object trong `depends-on` chưa được khai báo, hoặc phụ thuộc vòng (`a -> b -> a`), sẽ ném `ImportConfigurationException` và ứng dụng không chạy lên. Với Excel, API import dùng `depends-on` để import nhiều sheet trong một file (xem bên dưới).
- **Override từng bước:** mỗi bước (đọc file, map dòng, map ô, resolve reference, insert theo lô) là một named action trong `ObjectActionNamed`, nên có thể override bằng handler.
- **Transaction:** mỗi lô và mỗi lần đổi trạng thái chạy trong `REQUIRES_NEW`. Lô lỗi được chạy lại từng dòng một.
- **Import lỗi:** job chuyển sang `FAILED`, kèm `errorMessage` và `completedAt`.

### Import nhiều sheet trong một file Excel

Ví dụ một file có sheet `Order` và `OrderItem`:

```yaml
imports:
  objects:
    v2order:
      source: { sheet: Order }
    v2orderitem:
      source: { sheet: OrderItem }
      depends-on: [v2order]
```

Tạo job với `objectName` là object gốc (`v2order`), upload file và gọi API import như bình thường: `POST /generic/objects/v2order/imports/excel/{importJobId}`.

Cách chạy:

- Sheet của `v2order` được validate và import như một object đơn. Nếu job của nó **`COMPLETED`**, thư viện xử lý tiếp các object có `depends-on` tới `v2order` (trực tiếp hoặc gián tiếp), theo thứ tự phụ thuộc. Object phụ thuộc **bắt buộc** có `source.sheet`.
- Mỗi object được validate **và import xong** rồi mới tới object sau, vì sheet con tra mã của dòng cha trong database.
- **Cả file là một job** (job của `v2order`). Mỗi sheet là một lượt chạy (`ImportJobService.startSheetRun`) có `runId` riêng: dòng staging và lỗi của sheet mang `import_id = runId` (sheet gốc dùng chính `jobId`), còn bộ đếm (`totalRows`, `validRows`, `successRows`, `errorRows`) và trạng thái cộng dồn vào job. Handler của service chạy với `resource` là object của sheet và nhận `runId` làm `jobId`; cần job thì gọi `ImportJobService.jobIdOf(runId)`.
- Job lưu tóm tắt từng sheet ở `fileInfo.sheets: [{ objectName, sheet, runId, status, totalRows, validRows, successRows, errorRows, errorMessage }]` (số dòng là phần của riêng sheet đó).
- Sheet chưa `COMPLETED` (validate lỗi, import có lỗi hoặc lỗi hệ thống) dừng cả lượt: các sheet sau bị `SKIPPED`. Sheet của object phụ thuộc không có trong file cũng bị `SKIPPED`.
- Sheet phụ thuộc lỗi (`VALIDATION_FAILED` / `FAILED`): job `COMPLETED_WITH_ERRORS`, `errorMessage = "<sheet>: <lỗi>"`. Các sheet trước nó **đã được ghi** (mỗi sheet commit riêng).
- Tải file lỗi theo job: lỗi của từng sheet ghi vào cột `Result` của **đúng sheet đó** trong một lần ghi file. Lấy lỗi một sheet trên màn hình: lọc `importerror` theo `importId = runId`.
- Response là job của `v2order`, thêm `sheets` (như `fileInfo.sheets`). `sheetJobs` (chỉ các sheet phụ thuộc, `importJobId` = job của file) còn giữ cho client cũ. Lỗi của sheet phụ thuộc nằm trong job, không làm API trả lỗi.
- API chạy đồng bộ trong request và không có `@PreAuthorize`, giống trước đây.
- Có thể override toàn bộ luồng bằng handler cho action `ObjectActionNamed.Excel.IMPORT_FILE` / `Csv.IMPORT_FILE` (mặc định: `ImportFileService`).

### Export

Trạng thái: `PENDING_EXPORT → EXPORTING → COMPLETED | FAILED`.

- Gọi `POST /generic/objects/exportjob` với body `{ objectName, filterCriteria }`. File được tạo từ template Excel trên S3 (`exports.template` hoặc `exports.objects.<obj>.template`), rồi stream lên S3. Export chạy bất đồng bộ trên executor `dataExchangeTaskExecutor`, nên service cần bật `@EnableAsync`.
- `GET /generic/objects/exportjob/{id}` trả về `downloadUrl` (presigned).

**Đường dẫn file trên S3:** `{tenantCode}/{aws.s3.sub-folder}/{objectName}/{yyyyMMdd}/{uuid}_{epochMillis}.{ext}`.

## Gọi service khác

- **`GenericObjectInternalService`:** gọi API nội bộ của service khác tại `objects.api-config.service-urls.<service> + "/" + <service>`; service không khai báo (hoặc giá trị trống) thì dùng `objects.api-config.base-url + "/" + <service>`.
  - Key của `service-urls` là tên service trong `ObjectServiceFactory`: `user`, `customer`, `order`, `inventory`, `master-data`, `catalog`.
  - Đường dẫn `/internal` thường bị gateway công khai chặn: trỏ `service-urls` vào host nội bộ của từng service.
  - Service đích được tra trong `ObjectServiceFactory`. **Object mới gọi chéo service phải được thêm vào map này** với key viết thường; nếu không, lời gọi ném `IllegalStateException` có tên object.
- **`ObjectClientService`:** class trừu tượng. Kế thừa class này và implement `getBaseUrl()` để có sẵn các hàm filter / getById / query / conversion tới `/api/v1/internal`.
- **Tích hợp hệ thống ngoài:** dùng `integrationClient.connector(<name>)` với cấu hình `integration.connectors.<name>`.
  - OAuth token được cache trong Redis.
  - Request được ghi vào bảng `integration_log`.
  - Muốn ghi log cho một endpoint thì gắn `@IntegrationLogging(connector, operation)`.

## Lỗi

- **Tạo lỗi:** dùng factory `ObjectErrors` (`notFound`, `validateException`, `payloadInvalidException`…).
- **Mã lỗi:** khai báo trong `ObjectErrorCode`.
- **Chi tiết lỗi validate:** `ObjectErrorDetail { code, params, message }`. Message validate lấy từ commons và là tiếng Việt.

## Build & phát hành

```bash
./gradlew compileJava
./gradlew clean publishToMavenLocal -x test   # giống lệnh JitPack chạy
```

1. Muốn nâng commons thì sửa `msmCommonVersion` trong `build.gradle.kts`. Commons phải có tag và build thành công trên JitPack trước.
2. Merge thay đổi vào `main`.
3. Tạo GitHub Release với tag mới (semver, không có tiền tố `v`).
4. Mở <https://jitpack.io/#msm-lib/generic-object-management> và bấm **Get it** ở tag mới để build.

`jitpack.yml` cố định JDK `21.0.2-open` qua SDKMAN.

## Hạn chế đã biết

- `MasterDataApiService` và chế độ chạy localhost có URL viết cứng trong code.
- `ExportJobMeta.TABLE` dùng schema `"bhc"`, trong khi các Meta khác không có schema.
- Test tự động mới chỉ có cho phần cấu hình import, `ImportFileService` và `FileReaderService.getSheetNames` (`./gradlew test`).
