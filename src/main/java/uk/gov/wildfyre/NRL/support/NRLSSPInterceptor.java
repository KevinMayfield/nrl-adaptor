package uk.gov.wildfyre.NRL.support;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import io.jsonwebtoken.Jwts;
import uk.gov.wildfyre.NRL.HapiProperties;

import java.io.IOException;
import java.util.*;

public class NRLSSPInterceptor implements IClientInterceptor {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NRLSSPInterceptor.class);

    @Override
    public void interceptRequest(IHttpRequest iHttpRequest) {


        iHttpRequest.addHeader("fromASID", HapiProperties.getNhsAsidFrom());


        iHttpRequest.addHeader("toASID",HapiProperties.getNhsAsidTo());

      //  iHttpRequest.addHeader("Ssp-TraceID", UUID.randomUUID().toString());

        Date exp = new Date(System.currentTimeMillis() + 300000);
        Date iat = new Date(System.currentTimeMillis());

        // Build registered and custom Claims.
        CreatePayloadData createPayloadData = new CreatePayloadData();
        String jsonString = createPayloadData.buildPayloadData(exp, iat, false);
        String compactJws = Jwts.builder()
                .setHeaderParam("alg", "none")
                .setHeaderParam("typ", "JWT")
                .setPayload(jsonString)
                .compact();
        log.info("STU3 JWT Created");
        iHttpRequest.addHeader("Authorization", "Bearer " + compactJws);

        iHttpRequest.removeHeaders("Accept");

        iHttpRequest.addHeader("Accept", "application/fhir+json");



        Map<String, List<String>> headers = iHttpRequest.getAllHeaders();
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
    }

    @Override
    public void interceptResponse(IHttpResponse iHttpResponse) throws IOException {

    }
}
