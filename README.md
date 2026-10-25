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
- `audit_log_outbox`, nếu bật change log (không phải JPA entity, xem mục Change log)

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
  - Hai bean cùng một class (`InternalObjectQueryRepository` chỉ là lớp con giữ tên cũ), khác nhau ở `ObjectQuery`.
- **Thứ tự xử lý khi ghi:** audit (`AuditStrategy`), rồi đóng gói `customValues`, rồi insert/update. Khi đọc: trải phẳng `customValues` ra map gốc.
- **Update:** gộp dữ liệu mới vào bản ghi hiện có rồi **ghi đè cả dòng**, không phải partial patch. Nếu object có `version`, payload phải gửi kèm `version`.
- **Giá trị mặc định và chuẩn hóa text** (populate) chỉ chạy khi **create**.
- **Cập nhật nhiều dòng bằng biểu thức** (`updateWithExpressions(objectName, List<ExpressionUpdate>)`, cần commons 2.4.2): mỗi phần tử là một dòng theo id, giá trị có thể là biểu thức jOOQ (`quantity + 5`) và điều kiện chặn riêng (`guard`). Tất cả chạy trong một batch JDBC, mỗi dòng `version + 1`. Dòng không được cập nhật (sai id, không qua guard) có count 0, không ném lỗi. Có change log thì chỉ đọc các dòng một lần trước và một lần sau batch; một id cập nhật nhiều lần trong batch chỉ ghi một entry, từ giá trị trước batch tới giá trị cuối.
- **Delete:** xóa mềm nếu metadata có `isDeleted`/`deletedAt`/`deletedBy`/`deletedById`, nếu không thì xóa thật.
- **`code` bị thiếu khi create:** tự sinh ngẫu nhiên. Nếu object có trong `ObjectConstants.PREFIX_OBJECT_CODE` thì sinh theo tiền tố.
- **Nguồn metadata:** `ObjectMetadataFactory` (từ JPA) trước, sau đó đến bảng `object_metadata` (được cache).
- **Người dùng và tenant:** luôn lấy từ `RequestContextHolder` của commons.

## Change log (audit dữ liệu)

Mỗi dòng ghi qua `objectQueryRepository` / `internalObjectQueryRepository` (route CRUD generic, import, code service gọi repository) của object **được bật** sinh một entry: ai, lúc nào, CREATE / UPDATE / DELETE, giá trị trước / sau của từng field đổi. Entry ghi vào bảng outbox `audit_log_outbox` của service **trong cùng transaction** với dòng dữ liệu, rồi một job đẩy sang `POST /notification/api/v1/internal/audit-logs/batch` (bảng `audit_logs` của notification, category `DATA_CHANGE`).

```yaml
objects:
  api-config:
    service-urls:
      notification: ${NOTIFICATION_SERVICE_URL:}   # host nội bộ, không kèm /notification
  change-log:
    enabled: true
    source-service: ORDER_SERVICE      # AuditServiceEnum của notification
    default-tenant: bhc                # tenant của entry ghi ngoài request (job)
    mask-fields: [password]            # thay giá trị bằng ***
    objects: [v2order, v2orderitem, kpiallocate]   # chỉ object có tên ở đây mới được ghi log
    object-options:                    # tùy chọn riêng, chỉ cho object cần
      v2orderitem:
        parent-field: orderId          # entityParentId
      kpiallocate:
        code-field: code               # entityCode (mặc định "code")
        ignore-fields: [lastSyncAt]
    relay:
      interval: 30s
      batch-size: 200
      max-attempts: 10
      retention: 7d                    # entry đã gửi bị xóa sau thời gian này
```

**Migration** (mỗi service bật change log tự tạo bảng, không có FK):

```sql
create table if not exists audit_log_outbox (
    id               uuid primary key,
    created_at       timestamptz  not null,
    tenant_code      varchar(60),
    source_service   varchar(60),
    tx_id            varchar(100),
    action           varchar(20)  not null,
    entity_type      varchar(100) not null,
    entity_id        varchar(100),
    entity_parent_id varchar(100),
    entity_code      varchar(255),
    actor            jsonb,
    changed_fields   jsonb,
    metadata         jsonb,
    status           varchar(20)  not null default 'PENDING',
    attempts         integer      not null default 0,
    next_attempt_at  timestamptz  not null default now(),
    locked_until     timestamptz,
    sent_at          timestamptz,
    last_error       text
);
create index if not exists idx_audit_log_outbox_status on audit_log_outbox (status, next_attempt_at);
create index if not exists idx_audit_log_outbox_entity on audit_log_outbox (entity_type, entity_id);
```

**Hành vi:**

- **Ghi log hỏng thì việc ghi dữ liệu hỏng theo:** entry được insert trong `beforeCommit` của transaction đang chạy; insert lỗi thì cả transaction rollback, nên không có thay đổi nào được commit mà thiếu log. Không có transaction (route `NOT_SUPPORTED`, job) thì mỗi lệnh ghi tự commit và entry được insert ngay sau nó.
- **Field so sánh** ở dạng field name (đã trải `customValues`): field của metadata trước, sau đó các key khác theo tên. Đổi từ `null` sang có giá trị cũng tính là thay đổi. Số so theo giá trị (`10` = `10.000`). Mặc định bỏ qua `version`, `created_*`, `updated_*`, `deleted_*`, `customValues`. Update không đổi field nào thì không có entry.
- **CREATE** liệt kê mọi field có giá trị. **DELETE** (`metadata.deleteType` SOFT / HARD) liệt kê giá trị của dòng trước khi xóa. Update làm `isDeleted` thành `true` cũng là DELETE SOFT.
- **Method không đọc dòng cũ** (`updateWithCondition`, `updateWithExpressions`, `upsert`, `bulkUpsertReturning`, `delete` / `batchDelete` theo danh sách): khi object được bật, repository đọc các dòng bị ảnh hưởng trước và sau khi ghi bằng `internalQueryService` (không lọc data scope). Upsert theo tên constraint tra cột của constraint trong `pg_constraint` (cache khi tìm thấy; không tìm thấy thì không cache và lần sau tra lại); không tra được cột (sai tên, constraint không thuộc bảng trong `search_path`, cột không có trong metadata) thì upsert bị từ chối bằng `IllegalStateException`, để không ghi nhầm thành CREATE. `insertBatch*` tự gán `id` cho dòng chưa có, vì insert không trả dòng về.
- **Người ghi** lấy từ `RequestContextHolder`; không có user (job, lời gọi nội bộ) thì actor là `SYSTEM`. `txId` là trace id của request, không có thì là id chung của transaction.
- **Relay** chạy trên thread riêng của lib (service không cần `@EnableScheduling`), nhiều instance chạy cùng lúc được (`FOR UPDATE SKIP LOCKED`). Batch bị notification từ chối (4xx) được gửi lại từng entry; entry lỗi thử lại với độ trễ tăng dần (tối đa 1 giờ), quá `max-attempts` thì ở trạng thái `FAILED` để xử lý tay. Giao **ít nhất một lần**: gửi thành công mà chưa kịp đánh dấu SENT thì lần sau gửi lại.
- **Không được ghi log:** dòng ghi bằng JPA, jOOQ / SQL viết tay, `@Modifying` của service (không đi qua repository của lib).

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
- **Dòng tiêu đề hợp lệ** khi có ít nhất một cột trùng field của object và số cột được nhận (kể cả cột mà handler `@ImportDetectHeaderHandler` của service map sang tên không phải field, ví dụ mỗi KPI một cột) chiếm ít nhất một nửa số cột của dòng.
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

- Sheet của `v2order` được validate và import như một object đơn. Nếu nó được import (`COMPLETED` hoặc `COMPLETED_WITH_ERRORS`), thư viện xử lý tiếp các object có `depends-on` tới `v2order` (trực tiếp hoặc gián tiếp), theo thứ tự phụ thuộc. Object phụ thuộc **bắt buộc** có `source.sheet`.
- Mỗi object được validate **và import xong** rồi mới tới object sau, vì sheet con tra mã của dòng cha trong database.
- **Cả file là một job** (job của `v2order`). Mỗi sheet là một lượt chạy (`ImportJobService.startSheetRun`) có `runId` riêng: dòng staging và lỗi của sheet mang `import_id = runId` (sheet gốc dùng chính `jobId`), còn bộ đếm (`totalRows`, `validRows`, `successRows`, `errorRows`) và trạng thái cộng dồn vào job. Handler của service chạy với `resource` là object của sheet và nhận `runId` làm `jobId`; cần job thì gọi `ImportJobService.jobIdOf(runId)`.
- Job lưu tóm tắt từng sheet ở `fileInfo.sheets: [{ objectName, sheet, runId, status, totalRows, validRows, successRows, errorRows, errorMessage }]` (số dòng là phần của riêng sheet đó).
- **Lỗi khi validate thì dừng, lỗi khi import thì chạy tiếp:** sheet `VALIDATION_FAILED` (hoặc `FAILED`, lỗi hệ thống cả bước import) dừng cả lượt, các sheet sau bị `SKIPPED`; sheet `COMPLETED_WITH_ERRORS` (một số dòng lỗi lúc ghi) không dừng. Sheet của object phụ thuộc không có trong file cũng bị `SKIPPED`. Mỗi lượt so số dòng lỗi của job với lúc lượt bắt đầu (`ImportJobService.errorRowsBefore`), nên lỗi của sheet trước không làm sheet sau thành `VALIDATION_FAILED`.
- Sheet gốc đã được import mà có sheet nào lỗi: job `COMPLETED_WITH_ERRORS`, `errorMessage` liệt kê `"<sheet>: <lỗi>"`. Các sheet trước sheet lỗi **đã được ghi** (mỗi sheet commit riêng).
- Tải file lỗi theo job: lỗi của từng sheet ghi vào cột `Result` của **đúng sheet đó** trong một lần ghi file. Lấy lỗi một sheet trên màn hình: lọc `importerror` theo `importId = runId`.
- Response là job của `v2order`, thêm `sheets` (như `fileInfo.sheets`). `sheetJobs` (chỉ các sheet phụ thuộc, `importJobId` = job của file) còn giữ cho client cũ. Lỗi của sheet phụ thuộc nằm trong job, không làm API trả lỗi.
- API chạy đồng bộ trong request và không có `@PreAuthorize`, giống trước đây.
- Có thể override toàn bộ luồng bằng handler cho action `ObjectActionNamed.Excel.IMPORT_FILE` / `Csv.IMPORT_FILE` (mặc định: `ImportFileService`).

### Import detail cho một header (`import_details`)

Người dùng đang ở một header (ví dụ một đơn hàng) và import các dòng detail của **đúng header đó**; file không cần cột khóa cha.

Cấu hình trên object detail:

```yaml
imports:
  objects:
    v2orderitem:
      source: { sheet: OrderItem }
      depends-on: [v2order]                  # import header + detail trong một file (mục trên), không bắt buộc
      parent:
        object: v2order                      # object header
        field: orderId                       # khóa cha trên detail; thư viện tự điền
        reference-fields: [id, code, name]   # snapshot <field>Reference (field phải có attributeRef), lấy từ header
        required: true                       # detail chỉ import được qua header
        identity: [productId, unitId]        # identity trong phạm vi một header (field khóa cha tự đứng đầu)
        modes: [APPEND, UPSERT]              # APPEND = chỉ thêm; UPSERT = thêm hoặc sửa trong phạm vi header
        default-mode: APPEND
        max-rows: 2000                       # 0 = không giới hạn
```

Gọi:

1. `POST /generic/objects/importjob` với `objectName` = object detail (`v2orderitem`), upload file Excel lên `uploadUrl` (như mọi import).
2. `PUT /generic/objects/v2order/{orderId}/import_details` với `{ "importJobId": "…", "version": <version header đang hiển thị>, "mode": "APPEND" }`. Route kiểm quyền action `import_details` trên object header (`canProcessAction`): user cần quyền `UPDATE` trên object header.
3. Response `{ "job": …, "parent": … }` (header đọc lại sau import); có lỗi thì tải file lỗi theo `jobId` như thường.

Thư viện (`ImportDetailsService`):

- Đọc header bằng repository có data scope của user (không thấy → 404), so `version` (lệch → 409); kiểm job (`UPLOADING`, object có `parent.object` = header, chưa gắn header khác, file Excel) và `mode` → 400.
- Gọi `import_parent_check` (`resource` = object detail) để service từ chối nếu header không còn sửa được, rồi lưu header vào `fileInfo.context` của job (`ImportContext`).
- Validate: mọi dòng được điền `field` = id header và `<field>Reference` = snapshot; file có cột khóa cha khác header → lỗi dòng `PARENT_MISMATCH`; identity trong phạm vi header, trùng trong file → `DUPLICATE_IN_FILE`; `APPEND` và dòng trùng detail sẵn có của header → `DUPLICATE_IN_PARENT`; quá `max-rows` → job `VALIDATION_FAILED`. Có lỗi thì không ghi gì.
- Import như thường (mỗi lô một transaction, lô lỗi thử lại từng dòng; hook `bulk_*` của service vẫn chạy). `UPSERT` sửa đúng detail của header.
- Route action chạy trong transaction (`GenericObjectService.objectAction`); `import_details` tạm dừng transaction đó (`NOT_SUPPORTED`) như đường `POST /{object}/imports/...`, vì job được cập nhật trong các transaction riêng khi ghi lô. `import_parent_completed` chạy trong một transaction riêng (`REQUIRES_NEW`).
- Có dòng được ghi → gọi `import_parent_completed` **một lần** (`resource` = object detail) để service tính lại header; handler lỗi → job `COMPLETED_WITH_ERRORS`.
- `required: true`: `POST /generic/objects/{detail}/imports/{type}/{jobId}` không qua header bị từ chối (400). Sheet detail của file header + detail vẫn chạy (là lượt của job header).

Điểm mở rộng của service (theo kiểu `@ImportRowMappingHandler`):

```java
@ImportParentCheckHandler(resource = "v2orderitem")
public void checkOrder(ActionContext<ImportParentContext> context) { /* throw để từ chối */ }

@ImportParentCompletedHandler(resource = "v2orderitem")
public void recomputeOrder(ActionContext<ImportParentResult> context) { /* tính lại tổng, lịch sử… */ }
```

Trong các handler / hook khác, `ImportJobService.contextOf(importId)` trả header của job (null khi import thường).

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
