package uk.gov.wildfyre.nrl.providers;

import ca.uhn.fhir.rest.server.IResourceProvider;

public interface ICCResourceProvider extends IResourceProvider {

    Long count();
}
