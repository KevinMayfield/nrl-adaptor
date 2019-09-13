package uk.gov.wildfyre.nrl.interceptor;

import com.google.gson.JsonObject;
import org.jglue.fluentjson.JsonBuilderFactory;
import org.jglue.fluentjson.JsonObjectBuilder;

import java.util.Date;

public class CreatePayloadData {

    public String buildPayloadData(Date exp, Date iat, String verbName) {


        String scope = "patient/DocumentReference.read";
        switch (verbName) {
            case "POST":
            case "PUT":
            case "DELETE":
                scope = "patient/DocumentReference.write";
                break;
            default:
                break;

        }

        // Fluent Json is a Java fluent builder for creating JSON using Google Gson
        JsonObject jsonObject = JsonBuilderFactory.buildObject() //

                // Registered Claims
                .add("iss", "https://demonstrator.com") //
                .add("sub", "https://fhir.nhs.uk/Id/sds-role-profile-id|fakeRoleId") //
                .add("aud", "https://nrls.com/fhir/documentreference") //
                .add("exp", exp.getTime()/1000) //
                .add("iat", iat.getTime()/1000) //

                // Custom Claims
                .add("reason_for_request", "directcare") //
                .add("scope", scope) //
                .add("requesting_system", "https://fhir.nhs.uk/Id/accredited-system|200000000117") //
                .add("requesting_organization", "https://fhir.nhs.uk/Id/ods-organization-code|AMS01")
                .add("requesting_user", "https://fhir.nhs.uk/Id/sds-role-profile-id|fakeRoleId")
                .getJson();

        return jsonObject.toString();

    }



    public JsonObjectBuilder<?,JsonObject> getName(String nameType, String name) {
        return JsonBuilderFactory.buildObject()
                .addArray(nameType) //
                .add(name)
                .end();
    }
}
