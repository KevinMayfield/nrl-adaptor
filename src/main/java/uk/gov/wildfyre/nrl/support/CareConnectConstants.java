package uk.gov.wildfyre.nrl.support;

public final class CareConnectConstants {

    private CareConnectConstants() { }

    public static final  class IdentifierSystem {

        private IdentifierSystem () {}

        public final static String NHS_NUMBER =  "https://fhir.nhs.uk/Id/nhs-number";

        public final static String SDS_ORGANISATION_CODE = "https://fhir.nhs.uk/Id/ods-organization-code";
    }

    public final static String JSON_CONTENT_TYPE = "application/json+fhir;charset=UTF-8";
}
