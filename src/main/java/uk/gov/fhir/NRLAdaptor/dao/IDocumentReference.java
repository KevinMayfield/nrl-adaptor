package uk.gov.fhir.NRLAdaptor.dao;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;

import java.util.List;

public interface IDocumentReference {


    DocumentReference read(IGenericClient client, IdType internalId);

    List<DocumentReference> search(IGenericClient client, ReferenceParam patient) throws Exception;


}
