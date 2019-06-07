package uk.gov.fhir.NRLAdaptor.providers;

import ca.uhn.fhir.rest.server.IResourceProvider;

public interface ICCResourceProvider extends IResourceProvider {

    Long count();
}
