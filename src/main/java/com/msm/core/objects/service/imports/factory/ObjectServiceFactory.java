package com.msm.core.objects.service.imports.factory;

import java.util.HashMap;
import java.util.Map;

public class ObjectServiceFactory {
    private static final Map<String, String> OBJECT_SERVICE_FACTORY = new HashMap<>();

    static {

        //============ customer
        registerObjectService("bankaccount", "customer");
        registerObjectService("customerproductdemand", "customer");
        registerObjectService("employees", "customer");
        registerObjectService("smstracing", "customer");
        registerObjectService("channels", "customer");
        registerObjectService("retailers", "customer");
        registerObjectService("accountsiteemployee", "customer");
        registerObjectService("contracttype", "customer");
        registerObjectService("jobcrawldataupdatehistory", "customer");
        registerObjectService("arpaymenttransaction", "customer");
        registerObjectService("integrationlog", "customer");
        registerObjectService("accountsite", "customer");
        registerObjectService("accountattribute", "customer");
        registerObjectService("customerbalance", "customer");
        registerObjectService("distributors", "customer");
        registerObjectService("customers", "customer");
        registerObjectService("shops", "customer");
        registerObjectService("saleroutesemployees", "customer");
//        registerObjectService("locations", "customer");
        registerObjectService("customerproductdemanddetail", "customer");
        registerObjectService("contract", "customer");
        registerObjectService("arpayment", "customer");
        registerObjectService("employeesroles", "customer");
        registerObjectService("saleroutesdistributor", "customer");
//        registerObjectService("roles", "customer");
        registerObjectService("account", "customer");
        registerObjectService("ardocumentdetail", "customer");
        registerObjectService("salelineorganizationchart", "customer");
        registerObjectService("jobmigrationhistory", "customer");
        registerObjectService("saleroutes", "customer");
        registerObjectService("distributoraddresses", "customer");
        registerObjectService("productgroups", "customer");
        registerObjectService("ardocument", "customer");
        registerObjectService("customertype", "customer");
        registerObjectService("contact", "customer");
        registerObjectService("customerattributes", "customer");
        registerObjectService("employee", "customer");
        registerObjectService("saleroutesretailers", "customer");
        registerObjectService("salerouteproductgroups", "customer");
        registerObjectService("retaileraddresses", "customer");
        registerObjectService("profile", "customer");
        registerObjectService("channelgroups", "customer");
        registerObjectService("contractproduct", "customer");


        //============ order
        registerObjectService("voucherconditionitem", "order");
        registerObjectService("promotionproducts", "order");
        registerObjectService("delivery", "order");
        registerObjectService("promotioncustomerpromotionusage", "order");
        registerObjectService("vouchercustomerusages", "order");
        registerObjectService("blanketordertype", "order");
        registerObjectService("returnreason", "order");
        registerObjectService("orderitementity", "order");
        registerObjectService("v2orderitem", "order");
        registerObjectService("v2deliveryschedule", "order");
        registerObjectService("vouchercode", "order");
        registerObjectService("ordertype", "order");
        registerObjectService("promotionchannelmapping", "order");
        registerObjectService("shipment", "order");
        registerObjectService("deliveryscheduletype", "order");
        registerObjectService("promotionprogram", "order");
        registerObjectService("voucherfileinfo", "order");
        registerObjectService("shipmentproduct", "order");
        registerObjectService("v2order", "order");
        registerObjectService("orderentity", "order");
        registerObjectService("outboxentity", "order");
        registerObjectService("v2blanketorder0", "order");
        registerObjectService("v2blanketorderproduct", "order");
        registerObjectService("promotionproductgifts", "order");
        registerObjectService("promotionusertransaction", "order");
        registerObjectService("v2deliveryscheduleproduct", "order");
        registerObjectService("promotionshopmapping", "order");
        registerObjectService("voucherrule", "order");
        registerObjectService("v2deliveryaddress", "order");
        registerObjectService("v2blanketorderproductallocate", "order");
        registerObjectService("deliveryproduct", "order");
        registerObjectService("v2blanketorder", "order");


        //============ master-data
        registerObjectService("objectconversion", "master-data");
        registerObjectService("recordtypes", "master-data");
        registerObjectService("bank", "master-data");
        registerObjectService("itemtaxgroup", "master-data");
        registerObjectService("taxgroup", "master-data");
        registerObjectService("apiconfigs", "master-data");
        registerObjectService("objectdependency", "master-data");
        registerObjectService("geographylocation", "master-data");
        registerObjectService("paymentterm", "master-data");
        registerObjectService("paymentmethod", "master-data");
        registerObjectService("competitor", "master-data");
        registerObjectService("exchangerate", "master-data");
        registerObjectService("unit", "master-data");
        registerObjectService("objectentry", "master-data");
        registerObjectService("locations", "master-data");
        registerObjectService("attachment", "master-data");
        registerObjectService("rules", "master-data");
        registerObjectService("currency", "master-data");
        registerObjectService("attributegroups", "master-data");
        registerObjectService("objectreferencemetadata", "master-data");
        registerObjectService("geographytype", "master-data");
        registerObjectService("attributes", "master-data");
        registerObjectService("tax", "master-data");
        registerObjectService("objectstage", "master-data");
        registerObjectService("attributelistvalues", "master-data");


        //========== user
        registerObjectService("position", "user");
        registerObjectService("teammember", "user");
        registerObjectService("department", "user");
//        registerObjectService("attributes", "user"); // duplicate at "master-data"
        registerObjectService("invitation", "user");
        registerObjectService("msmuser", "user");
        registerObjectService("functions", "user");
        registerObjectService("userorganization", "user");
        registerObjectService("team", "user");
        registerObjectService("portaluserrole", "user");
        registerObjectService("portaluser", "user");
        registerObjectService("rolemodulepermission", "user");
        registerObjectService("organization", "user");
        registerObjectService("portalmodule", "user");
        registerObjectService("menuaction", "user");
        registerObjectService("menu", "user");
//        registerObjectService("integrationlog", "user");
        registerObjectService("organizationtype", "user");
        registerObjectService("roles", "user"); // Duplicate  at "customer"
//        registerObjectService("attributegroups", "user"); // Duplicate at "master-data"
        registerObjectService("portalrole", "user");
        registerObjectService("userrole", "user");
        registerObjectService("rolepermission", "user");


        // inventory
        registerObjectService("stocktransactions", "inventory");
        registerObjectService("shippingpricedetail", "inventory");
        registerObjectService("shippingprice", "inventory");
        registerObjectService("porterpricedetail", "inventory");
        registerObjectService("warehouse", "inventory");
        registerObjectService("truckload", "inventory");
        registerObjectService("route", "inventory");
        registerObjectService("stockdetailtransactions", "inventory");
        registerObjectService("site", "inventory");
        registerObjectService("inventories", "inventory");
        registerObjectService("zone", "inventory");
        registerObjectService("warehousechannel", "inventory");
        registerObjectService("center", "inventory");
        registerObjectService("warehouses", "inventory");
        registerObjectService("zonedetail", "inventory");
        registerObjectService("deliverymethod", "inventory");


    }

    public static String getServiceName(String objectType) {
        return OBJECT_SERVICE_FACTORY.get(objectType);
    }

    public static void registerObjectService(String objectType, String serviceName) {
        OBJECT_SERVICE_FACTORY.put(objectType, serviceName);
    }
}

