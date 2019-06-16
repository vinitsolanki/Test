package com.ivypay.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class RequestMappingJsonRead {

    public static final String URL_CONTEXT = "https://{{host}}:10006";

    public static final String VERSION = "/v1";
    public static final String PORTAL_CONTEXT = "/api/portal" + VERSION;
    public static final String SERVER_CONTEXT = "/api/server" + VERSION;
    public static final String BUSINESS_CONTEXT = "/api/business" + VERSION;
    public static final String MOBILE_CONTEXT = "/api/mobile" + VERSION;
    public static final String HEADERS = "Authorization:bearer {{token}}\n Content-Type:application/json\n apiKey:{{apikey}}";
    Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private String fields;

    public static void main(String[] args) throws JsonProcessingException {

        RequestMappingJsonRead requestMappingJsonRead = new RequestMappingJsonRead();
        requestMappingJsonRead.exportData();

    }

    public void exportData() throws JsonProcessingException {


        List<RequestMapping> customersMap = readCustomerData();

        PostManCollection postManCollection = new PostManCollection();
        postManCollection.setId(UUID.randomUUID().toString());
        postManCollection.setName("IvyPay_API");

        PostManFolder stsFolder = getFolder("STS");
        PostManFolder cspFolder = getFolder("CSP");
        PostManFolder mobileFolder = getFolder("Mobile");
        PostManFolder crpFolder = getFolder("CRP");

        List<PostManRequest> requests = customersMap.stream().map(requestMapping -> {

            PostManRequest request = new PostManRequest();
            request.setId(UUID.randomUUID().toString());
            request.setMethod(requestMapping.getMethod());
            request.setCollection_id(postManCollection.getId());
            request.setCollectionId(postManCollection.getId());
            request.setHeaders(HEADERS);
            request.setDataMode("raw");
            if (requestMapping.getMethod().equals("POST")) {
                request.setRawModeData(getFields(requestMapping.getBean()));
                request.setUrl(URL_CONTEXT.concat(requestMapping.getPath()));
            }else if (requestMapping.getMethod().equals("GET")) {
                request.setUrl(URL_CONTEXT.concat(requestMapping.getPath()) +  getParameters(requestMapping.getBean()));
            }

            if (requestMapping.getPath().startsWith(SERVER_CONTEXT)) {
                request.setFolder(stsFolder.getId());
                request.setName(requestMapping.getPath().replace(SERVER_CONTEXT + "/", ""));
                stsFolder.getOrder().add(request.getId());
            } else if (requestMapping.getPath().startsWith(PORTAL_CONTEXT)) {
                request.setFolder(cspFolder.getId());
                request.setName(requestMapping.getPath().replace(PORTAL_CONTEXT + "/", ""));
                cspFolder.getOrder().add(request.getId());
            } else if (requestMapping.getPath().startsWith(MOBILE_CONTEXT)) {
                request.setFolder(mobileFolder.getId());
                request.setName(requestMapping.getPath().replace(MOBILE_CONTEXT + "/", ""));
                mobileFolder.getOrder().add(request.getId());
            } else if (requestMapping.getPath().startsWith(BUSINESS_CONTEXT)) {
                request.setFolder(crpFolder.getId());
                request.setName(requestMapping.getPath().replace(BUSINESS_CONTEXT + "/", ""));
                crpFolder.getOrder().add(request.getId());
            }

            //public com.revogear.service.rest.bean.request.GenericResponseBean com.ivypay.api.crp.controller.transfer.CompleteMeController.reject(com.ivypay.api.crp.controller.transfer.bean.TransferAuthorizeRejectRequest)



            return request;
        }).collect(Collectors.toList());

        postManCollection.setFolders(Arrays.asList(stsFolder, cspFolder, mobileFolder, crpFolder));
        postManCollection.setFolders_order(Arrays.asList(stsFolder.getId(), cspFolder.getId(), mobileFolder.getId(), crpFolder.getId()));
        postManCollection.setRequests(requests);


        String collection = new ObjectMapper().writeValueAsString(postManCollection);
        System.out.println(collection);


//        RequestMappingJsonRead requestMappingJsonRead = new RequestMappingJsonRead();
//        Map<String, List<RequestMapping>> customers = requestMappingJsonRead.readCustomerData();
//
//        customers.entrySet().stream().forEach(RequestMappingJsonRead.printit);

    }

    private String getParameters(String bean) {
        StringBuilder sb = new StringBuilder("?");
        String fields = getFields(bean);
        try {

            if(fields.length() > 2) {
            HashMap<String, Object> map = new Gson().fromJson(fields, HashMap.class);

            for(HashMap.Entry<String, Object> e : map.entrySet()){
                if(sb.length() > 1){
                    sb.append('&');
                }

                sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode("value", "UTF-8"));

            }}

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return sb.toString();
    }

    private PostManFolder getFolder(String folderName) {
        PostManFolder folder = new PostManFolder();
        folder.setId(UUID.randomUUID().toString());
        folder.setName(folderName);
        return folder;
    }

    /**
     * print a Map.Entry
     */
    private static Consumer<Map.Entry<String, List<RequestMapping>>> printit = customer -> {
        final List<RequestMapping> custoemrs = customer.getValue();

        for (RequestMapping cust : custoemrs) {
            System.out.println("RequestMapping Details are ");
            System.out.println("========================================");
            System.out.println("RequestMapping Key is: " + customer.getKey());
            System.out.println("RequestMapping FirstName is: " + cust.getPath());
            System.out.println("RequestMapping LastName is: " + cust.getMethod());

            System.out.println();
        }
    };

    private String toJson(String s) {
        s = s.substring(0, s.length()).replace("{", "{\"");
        s = s.substring(0, s.length()).replace("}", "\"}");
        s = s.substring(0, s.length()).replace(", ", "\", \"");
        s = s.substring(0, s.length()).replace("=", "\":\"");
        s = s.substring(0, s.length()).replace("\"[", "[");
        s = s.substring(0, s.length()).replace("]\"", "]");
        s = s.substring(0, s.length()).replace("}\", \"{", "}, {");
        return s;
    }

    @SuppressWarnings("unchecked")
    public List<RequestMapping> readCustomerData() {
//        Map<String, List<RequestMapping>> customers = new LinkedHashMap<String, List<RequestMapping>>();

        List<RequestMapping> requestMappings = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        File customer = getCustomerFileReader.apply("mappings.json");
        JSONParser parser = new JSONParser();
        try (Reader is = new FileReader(customer)) {
            JSONObject jsonObject = (JSONObject) parser.parse(is);
            Set keys = jsonObject.keySet();
            HashMap<String, String> hashMap = null;
            for (Object object : keys) {
                String key = (String) object;
//                HashMap<String, Object> map = mapper.readValue(key, HashMap.class);
//                System.out.println("map " + map);
                JSONObject value = (JSONObject) jsonObject.get(key);

                key = key.substring(1);
                key = key.substring(0, key.length() - 1);
                String[] splitted = key.split(",");
                hashMap = new LinkedHashMap<String, String>(splitToMap(splitted[1]));
                String[] requestMappingsArray = splitRequestMappings(splitted[0]);
                if (requestMappingsArray.length > 3) {
                    System.out.println(key);
                }

                List<RequestMapping> list = new ArrayList<RequestMapping>();
                for (String requestMapping : requestMappingsArray) {
                    requestMappings.add(new RequestMapping(requestMapping, removeBracket(hashMap.get("methods")), (String) value.get("method")));
                }

//                customers.put(key, list);


            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
//        System.out.println("customers = " + customers);
        return requestMappings;
    }

    private String removeBracket(String str) {
        return str.replace("[", "").replace("]", "");
    }

    private String[] splitRequestMappings(String requestMapping) {
        String str = removeBracket(requestMapping);
        return str.split(" \\|\\| ");
    }

    private Map<String, String> splitToMap(String in) {
        if (nonNull(in)) {
            return Splitter.on(" ").withKeyValueSeparator("=").split(in);
        } else {
            return new HashMap<String, String>();
        }
        //return Splitter.on(" ").withKeyValueSeparator("=").split(in);
    }

    /**
     * Retrieve a file with specified name
     */
    public Function<String, File> getCustomerFileReader = filename -> {
        ClassLoader cl = getClass().getClassLoader();
        File customer = new File(cl.getResource(filename).getFile());
        return customer;
    };

    public String getFields(String requestMappings) {
        try {
            String methodName = requestMappings.split(" ")[2];
            String parametersStr = StringUtils.substringBetween(methodName, "(", ")");
            if (parametersStr != null) {

                String[] parameter = parametersStr.split(",");
                System.out.println(parameter[0]);

                if (StringUtils.isNotBlank(parameter[0]) && !parameter[0].equals("javax.servlet.http.HttpServletRequest") && !parameter[0].equals("org.springframework.web.multipart.MultipartFile") && !parameter[0].equals("boolean")) {
                    Class<?> cls = Class.forName(parameter[0]);
                    Object clsInstance = (Object) cls.newInstance();
                    String fields = gson.toJson(clsInstance).replace("null", "\"\"");
                    if(StringUtils.isNotBlank(fields)){
                        return fields;
                    } else {
                        return "{}";
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        };
        return "{}";
    }

//    /**
//     * Read the JSON entry and return customer Id
//     */
//    private Function<JSONObject, String> key_customer = c -> (String) ((JSONObject) c)
//            .get("Customer_id");
//
//    /**
//     * Read the JSON entry and return the request RequestMapping
//     */
//    @SuppressWarnings("unchecked")
//    private Function<JSONObject, RequestMapping> value_requestCustomer = json -> {
//        return new RequestMapping((String) json.get("bean"), (String) json.get("method"));
//    };

    public class PostManCollection {

        String id;
        String name;
        String description;
        List order = new ArrayList<>();
        List<PostManFolder> folders = new ArrayList<>();
        List<PostManRequest> requests = new ArrayList<>();
        List folders_order;
        boolean isPublic = true;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List getOrder() {
            return order;
        }

        public void setOrder(List order) {
            this.order = order;
        }

        public List<PostManFolder> getFolders() {
            return folders;
        }

        public void setFolders(List<PostManFolder> folders) {
            this.folders = folders;
        }

        public List<PostManRequest> getRequests() {
            return requests;
        }

        public void setRequests(List<PostManRequest> requests) {
            this.requests = requests;
        }

        public List getFolders_order() {
            return folders_order;
        }

        public void setFolders_order(List folders_order) {
            this.folders_order = folders_order;
        }

        public boolean isPublic() {
            return isPublic;
        }

        public void setPublic(boolean aPublic) {
            isPublic = aPublic;
        }
    }

    public class PostManFolder {

        String id;
        String name;
        String description;
        String collectionId;
        List<String> order = new ArrayList<String>();
        String owner;
        List<PostManFolder> folders_order = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCollectionId() {
            return collectionId;
        }

        public void setCollectionId(String collectionId) {
            this.collectionId = collectionId;
        }

        public List getOrder() {
            return order;
        }

        public void setOrder(List order) {
            this.order = order;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public List<PostManFolder> getFolders_order() {
            return folders_order;
        }

        public void setFolders_order(List<PostManFolder> folders_order) {
            this.folders_order = folders_order;
        }
    }

    public class PostmanHeader {

        String key;
        String value;
        String description;
        boolean enabled = true;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public class PostManRequest {

        String id;
        String name;
        String description;
        String collectionId;
        String rawModeData;
        String collection_id;
        String folder;
        String headers;
        List headerData = new ArrayList<>();
        String url;
        String queryParams;
        String preRequestScript;
        String pathVariables;
        String pathVariableData;
        String method;
        List data = new ArrayList<>();
        String dataMode;
        String tests;
        String currentHelper;
        List helperAttributes = new ArrayList<>();
        long time;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCollectionId() {
            return collectionId;
        }

        public void setCollectionId(String collectionId) {
            this.collectionId = collectionId;
        }

        public String getRawModeData() {
            return rawModeData;
        }

        public void setRawModeData(String rawModeData) {
            this.rawModeData = rawModeData;
        }

        public String getCollection_id() {
            return collection_id;
        }

        public void setCollection_id(String collection_id) {
            this.collection_id = collection_id;
        }

        public String getFolder() {
            return folder;
        }

        public void setFolder(String folder) {
            this.folder = folder;
        }

        public String getHeaders() {
            return headers;
        }

        public void setHeaders(String headers) {
            this.headers = headers;
        }

        public List getHeaderData() {
            return headerData;
        }

        public void setHeaderData(List headerData) {
            this.headerData = headerData;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getQueryParams() {
            return queryParams;
        }

        public void setQueryParams(String queryParams) {
            this.queryParams = queryParams;
        }

        public String getPreRequestScript() {
            return preRequestScript;
        }

        public void setPreRequestScript(String preRequestScript) {
            this.preRequestScript = preRequestScript;
        }

        public String getPathVariables() {
            return pathVariables;
        }

        public void setPathVariables(String pathVariables) {
            this.pathVariables = pathVariables;
        }

        public String getPathVariableData() {
            return pathVariableData;
        }

        public void setPathVariableData(String pathVariableData) {
            this.pathVariableData = pathVariableData;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public List getData() {
            return data;
        }

        public void setData(List data) {
            this.data = data;
        }

        public String getDataMode() {
            return dataMode;
        }

        public void setDataMode(String dataMode) {
            this.dataMode = dataMode;
        }

        public String getTests() {
            return tests;
        }

        public void setTests(String tests) {
            this.tests = tests;
        }

        public String getCurrentHelper() {
            return currentHelper;
        }

        public void setCurrentHelper(String currentHelper) {
            this.currentHelper = currentHelper;
        }

        public List getHelperAttributes() {
            return helperAttributes;
        }

        public void setHelperAttributes(List helperAttributes) {
            this.helperAttributes = helperAttributes;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }
    }

    public class RequestMapping {

        private String path;
        private String method;
        private String bean;

        public RequestMapping(String path, String method, String bean) {
            this.path = path;
            this.method = method;
            this.bean = bean;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getBean() {
            return bean;
        }

        public void setBean(String bean) {
            this.bean = bean;
        }
    }
}