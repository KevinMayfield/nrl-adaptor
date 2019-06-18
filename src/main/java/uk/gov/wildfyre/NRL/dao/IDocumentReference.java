package uk.gov.wildfyre.NRL.dao;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.IdType;
import uk.gov.wildfyre.NRL.support.OperationOutcomeException;

import java.util.List;

public interface IDocumentReference {


    DocumentReference read(IGenericClient client, IdType internalId) throws Exception;

    MethodOutcome delete(IGenericClient client, IdType internalId) throws Exception;

    MethodOutcome update(IGenericClient client, DocumentReference composition,
                             @IdParam IdType theId,
                             @ConditionalUrlParam String theConditional) throws Exception;

    List<DocumentReference> search(IGenericClient client, ReferenceParam patient,
            TokenParam type,
            ReferenceParam org,
                                   TokenParam id
                                   ) throws Exception;

    MethodOutcome create(IGenericClient client,
                         DocumentReference composition,
                         @IdParam IdType theId,
                         @ConditionalUrlParam String theConditional) throws OperationOutcomeException;

}
