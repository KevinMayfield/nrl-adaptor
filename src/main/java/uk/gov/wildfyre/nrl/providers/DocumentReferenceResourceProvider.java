package uk.gov.wildfyre.nrl.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.nrl.dao.IDocumentReference;
import uk.gov.wildfyre.nrl.support.OperationOutcomeFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Component
public class DocumentReferenceResourceProvider implements IResourceProvider {


    @Autowired
    FhirContext ctx;

    @Autowired
    IDocumentReference resourceDao;

    @Autowired
    IGenericClient client;


    private static final Logger log = LoggerFactory.getLogger(DocumentReferenceResourceProvider.class);

    @Override
    public Class<DocumentReference> getResourceType() {
        return DocumentReference.class;
    }


    @Read
    public DocumentReference read(@IdParam IdType internalId) throws Exception {


        DocumentReference documentReference = resourceDao.read(client, internalId);
        if (documentReference == null) {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new ResourceNotFoundException("No documentReference details found for documentReference ID: " + internalId.getIdPart()),
                    OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueType.NOTFOUND);
        }

        return documentReference;
    }


    @Create
    public MethodOutcome create(HttpServletRequest theRequest, @ResourceParam DocumentReference documentReference) throws Exception {

        MethodOutcome outcome =  resourceDao.create(client,documentReference, null,null);

       // log.info(outcome.toString());
        return outcome;

    }

    @Update
    public MethodOutcome update(HttpServletRequest theRequest, @ResourceParam DocumentReference documentReference, @IdParam IdType theId, @ConditionalUrlParam String theConditional, RequestDetails theRequestDetails)
            throws Exception {

        MethodOutcome outcome =  resourceDao.update(client,documentReference, theId, theConditional);
        //log.info(outcome.toString());
        return outcome;

    }


    @Delete
    public MethodOutcome delete(HttpServletRequest theRequest, @IdParam IdType theId, @ConditionalUrlParam String theConditional, RequestDetails theRequestDetails)
    throws Exception {

        MethodOutcome outcome =  resourceDao.delete(client,theId);
        //log.info(outcome.toString());
        return outcome;

    }

    @Search
    public List<DocumentReference> search(HttpServletRequest httpRequest,
                                  @OptionalParam(name = DocumentReference.SP_PATIENT) ReferenceParam patient,
                                          @OptionalParam(name = DocumentReference.SP_TYPE)      TokenParam type,
                                          @OptionalParam(name = DocumentReference.SP_CUSTODIAN)      ReferenceParam org,
                                          @OptionalParam(name = DocumentReference.SP_RES_ID) TokenParam id
    ) throws Exception {

        return resourceDao.search(client,patient, type, org, id);


    }


}
