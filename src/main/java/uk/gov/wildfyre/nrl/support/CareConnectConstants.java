package uk.gov.wildfyre.nrl.support;

public final class CareConnectConstants {

    private CareConnectConstants() { }

    public static final  class IdentifierSystem {

        private IdentifierSystem () {}

        public static final String NHS_NUMBER =  "https://fhir.nhs.uk/Id/nhs-number";

        public static final String SDS_ORGANISATION_CODE = "https://fhir.nhs.uk/Id/ods-organization-code";
    }

    public static final String JSON_CONTENT_TYPE = "application/json+fhir;charset=UTF-8";
}
