package inventory.mysql;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

public class ElasticSearch {

    private String connection;
    private String index;
    private String doc_type;

    // Construcor
    public ElasticSearch() {
        // Get es_connection_string, es_index, and es_doc_type
        connection = System.getenv("es_connection_string");

        // Optional
        index = System.getenv("es_index");
        if (index == null || index.equals("")) {
            index = "api";
        }

        doc_type = System.getenv("es_doc_type");
        if (doc_type == null || doc_type.equals("")) {
            doc_type = "items";
        }

    }

    // Subscribe to topic and start polling
    public void refresh_cache() {
        JSONArray rows = get_all_rows();
        load_rows_into_cache(rows);
    }

    // Get all rows from database
    public JSONArray get_all_rows() {
        JSONArray rows = null;
        try {
            // TODO: Use some SpringBoot data approach to get the data
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
            .url("http://localhost:8080/micro/inventory")
            .get()
            .build();

            Response response = client.newCall(request).execute();
            rows = new JSONArray(response.body().string());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return rows;
    }

    // Subscribe to topic and start polling
    public void load_rows_into_cache(JSONArray rows) {
        for (int i = 0; i < rows.length(); i++) {
            JSONObject jsonObj = rows.getJSONObject(i);
            System.out.println(jsonObj.toString());

            try {
                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                RequestBody body = RequestBody.create(mediaType, jsonObj.toString());

                // Build URL
                String url = String.format("%s/%s/%s/%s", connection, index, doc_type, jsonObj.getInt("id"));
                Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("content-type", "application/json")
                .build();

                Response response = client.newCall(request).execute();
                JSONObject resp = new JSONObject(response.body().string());
                boolean created = resp.getBoolean("created");

                System.out.printf("Item %s was %s\n\n", resp.getString("_id"), ((created == true) ? "Created" : "Updated"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}