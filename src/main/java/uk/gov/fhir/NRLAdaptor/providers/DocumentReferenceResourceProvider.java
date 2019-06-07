package uk.gov.fhir.NRLAdaptor.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.DocumentReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.fhir.NRLAdaptor.dao.IDocumentReference;
import uk.gov.fhir.NRLAdaptor.support.OperationOutcomeFactory;

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
    public DocumentReference read(@IdParam IdType internalId) {


        DocumentReference documentReference = resourceDao.read(client, internalId);
        if (documentReference == null) {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new ResourceNotFoundException("No documentReference details found for documentReference ID: " + internalId.getIdPart()),
                    OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueType.NOTFOUND);
        }

        return documentReference;
    }
    @Search
    public List<DocumentReference> search(HttpServletRequest httpRequest,
                                  @OptionalParam(name = DocumentReference.SP_PATIENT) ReferenceParam patient
    ) throws Exception {

        return resourceDao.search(client,patient);


    }


}
