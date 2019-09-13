package uk.gov.wildfyre.nrl.dao;

import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.hl7.fhir.dstu3.model.IdType;
import uk.gov.wildfyre.nrl.support.OperationOutcomeException;

import java.util.List;

public interface IDocumentReference {


    DocumentReference read(IGenericClient client, IdType internalId) ;

    MethodOutcome delete(IGenericClient client, IdType internalId) ;

    MethodOutcome update(IGenericClient client, DocumentReference composition,
                             @IdParam IdType theId,
                             @ConditionalUrlParam String theConditional);

    List<DocumentReference> search(IGenericClient client, ReferenceParam patient,
            TokenParam type,
            ReferenceParam org,
                                   TokenParam id
                                   ) ;

    MethodOutcome create(IGenericClient client,
                         DocumentReference composition,
                         @IdParam IdType theId,
                         @ConditionalUrlParam String theConditional) throws OperationOutcomeException;

}
