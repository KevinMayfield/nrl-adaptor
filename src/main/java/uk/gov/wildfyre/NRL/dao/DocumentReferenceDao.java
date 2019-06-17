package uk.gov.wildfyre.NRL.dao;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.NRL.support.OperationOutcomeException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Component
public class DocumentReferenceDao implements IDocumentReference {

    private static final Logger log = LoggerFactory.getLogger(DocumentReferenceDao.class);


    @Override
    public DocumentReference read(IGenericClient client, IdType internalId) throws Exception {

        log.info(internalId.getValue());
        List<DocumentReference> docs = search(client, null, null, null, new TokenParam().setValue(internalId.getIdPart()));
        if (docs.size() > 0) {
            return docs.get(0);
        }
        return null;
    }

    @Override
    public List<DocumentReference> search(IGenericClient client, ReferenceParam patient, TokenParam type,
                                          ReferenceParam org, TokenParam id) throws Exception {


        List<DocumentReference> documents = new ArrayList<>();
        Bundle result = null;

        IQuery query = null;

        if (patient != null) {
            String patientQry = patient.getValue();
            if (!patientQry.contains("https://demographics.spineservices.nhs.uk/STU3/Patient/")) {
                patientQry = "https://demographics.spineservices.nhs.uk/STU3/Patient/" + patient.getValue();
            }
            if (patient.getChain() != null && patient.getChain().contains("identifier")) {
                String[] ids = patient.getValue().split("|");
                if (ids.length > 1 && ids[0].contains("https://fhir.nhs.uk/Id/nhs-number")) {
                    patientQry = "https://demographics.spineservices.nhs.uk/STU3/Patient/" + ids[1];
                }
            }
            log.trace(patientQry);
            query = client.search().forResource(DocumentReference.class)
                    .where(DocumentReference.SUBJECT.hasId(patientQry));
        } else {
            if (id != null) {
                query = client.search().forResource(DocumentReference.class)
                        .where(DocumentReference.RES_ID.exactly().code(id.getValue()));
            } else {
                throw new UnprocessableEntityException("patient or _id must be supplied as search parameters");
            }

        }


        if (type != null) {
            query = query.and(DocumentReference.TYPE.exactly().systemAndValues(type.getSystem(), type.getValue()));
        }
        if (org != null) {
            String documentQry = org.getValue();


            if (!documentQry.contains("https://directory.spineservices.nhs.uk/STU3/Organization/")) {
                documentQry = org.getValue();
            } else {
                documentQry = org.getValue().replace("https://directory.spineservices.nhs.uk/STU3/Organization/","");
            }
            if (org.getChain() != null && org.getChain().contains("identifier")) {
                String[] ids = org.getValue().split("|");
                if (ids.length > 1 && ids[0].contains("https://fhir.nhs.uk/Id/ods-organization-code")) {
                    documentQry = ids[1];
                }
            }
            log.info(documentQry);
            query = query.and(DocumentReference.CUSTODIAN.hasChainedProperty(Organization.IDENTIFIER.exactly().systemAndValues("https://fhir.nhs.uk/Id/ods-organization-code", documentQry)) );
        }

        result = (Bundle) query.returnBundle(Bundle.class)
                .execute();


        if (result != null && result.getEntry().size() > 0) {
            for (Bundle.BundleEntryComponent entry : result.getEntry()) {
                if (entry.getResource() instanceof DocumentReference) {
                    DocumentReference documentReference = (DocumentReference) entry.getResource();
                    if (documentReference.hasSubject()) {
                        if (documentReference.getSubject().getReference().contains("https://demographics.spineservices.nhs.uk/STU3/Patient/")) {
                            String nhsNumber = documentReference.getSubject().getReference().replace("https://demographics.spineservices.nhs.uk/STU3/Patient/", "");
                            Reference ref = new Reference("Patient/" + nhsNumber);
                            ref.getIdentifier().setSystem("https://fhir.nhs.uk/Id/nhs-number").setValue(nhsNumber);
                            documentReference.setSubject(ref);
                        }
                    }
                    if (documentReference.hasAuthor()) {
                        List<Reference> refs = new ArrayList<>();
                        for (Reference ref : documentReference.getAuthor()) {
                            refs.add(getReference(ref));
                        }
                        documentReference.setAuthor(refs);
                    }
                    if (documentReference.hasCustodian()) {

                        documentReference.setCustodian(getReference(documentReference.getCustodian()));
                    }
                    documents.add(documentReference);
                }
            }
        }

        return documents;
    }

    @Override
    public MethodOutcome create(IGenericClient client, DocumentReference originalDocumentReference, IdType theId, String theConditional) throws OperationOutcomeException {

        DocumentReference documentReference = (DocumentReference) originalDocumentReference;
        if (theId == null) {
            documentReference.setId("");
        } else {
            documentReference.setId(theId.getValue());
        }

        if (documentReference.hasSubject()) {
            if (documentReference.getSubject().hasReference()) {
                if (documentReference.getSubject().getReference().startsWith("Patient")) {
                    documentReference.getSubject().setReference("https://demographics.spineservices.nhs.uk/STU3/" + documentReference.getSubject().getReference());
                }
                if (!documentReference.getSubject().getReference().contains("https://demographics.spineservices.nhs.uk/STU3/Patient/")) {
                    throw new UnprocessableEntityException("Invalid subject reference");
                }
            }
            if (documentReference.getSubject().hasIdentifier()) {
                if (documentReference.getSubject().getIdentifier().getSystem().equals("https://fhir.nhs.uk/Id/nhs-number")) {
                    documentReference.setSubject(new Reference("https://demographics.spineservices.nhs.uk/STU3/Patient/" + documentReference.getSubject().getIdentifier().getValue()));
                }
            }
            if (!documentReference.getSubject().getReference().contains("https://demographics.spineservices.nhs.uk/STU3/Patient/")) {
                throw new UnprocessableEntityException("Invalid subject reference");
            }
        } else {
            throw new UnprocessableEntityException("Subject must be present");
        }

        documentReference.setIndexed(new Date());

        if (documentReference.hasAuthor()) {
            Reference masterRef = new Reference();

            for (Reference ref : documentReference.getAuthor()) {
                if (ref.hasReference()) {
                    if (ref.getReference().startsWith("Organization")) {
                        masterRef.setReference("https://directory.spineservices.nhs.uk/STU3/" + ref);
                    } else {
                        masterRef.setReference(ref.getReference());
                    }
                }
                if (ref.hasIdentifier()) {
                    if (ref.getIdentifier().getSystem().equals("https://fhir.nhs.uk/Id/ods-organization-code")) {
                        masterRef.setReference("https://directory.spineservices.nhs.uk/STU3/Organization/" + ref.getIdentifier().getValue());
                    }
                }
            }
            if (!masterRef.hasReference() || !masterRef.getReference().contains("https://directory.spineservices.nhs.uk/STU3/Organization/")) {
                throw new UnprocessableEntityException("Invalid Author reference");
            } else {
                documentReference.setAuthor(new ArrayList<>());
                documentReference.getAuthor().add(masterRef);
            }
        } else {
            throw new UnprocessableEntityException("Author must be present");
        }

        if (documentReference.hasCustodian()) {
            Reference ref = documentReference.getCustodian();

            if (ref.hasReference()) {
                if (ref.getReference().startsWith("Organization")) {
                    ref.setReference("https://directory.spineservices.nhs.uk/STU3/" + ref);
                }
            }
            if (ref.hasIdentifier()) {
                if (ref.getIdentifier().getSystem().equals("https://fhir.nhs.uk/Id/ods-organization-code")) {
                    ref.setReference("https://directory.spineservices.nhs.uk/STU3/Organization/" + ref.getIdentifier().getValue());
                }
            }
            log.info(ref.getReference());
            if (!ref.hasReference() || !ref.getReference().contains("https://directory.spineservices.nhs.uk/STU3/Organization/")) {
                throw new UnprocessableEntityException("Invalid Custodian reference");
            } else {
                ref.setIdentifier(null);
            }
        } else {
            throw new UnprocessableEntityException("Custodian must be present");
        }

        documentReference.setType(null);

        documentReference.getType().addCoding()
                .setSystem("http://snomed.info/sct")
                .setDisplay("Mental Health Crisis Plan")
                .setCode("736253002");
        documentReference.setContext(null);

        System.out.println(FhirContext.forDstu3().newJsonParser().setPrettyPrint(true).encodeResourceToString(documentReference));

        MethodOutcome outcome = null;
        if (theId != null) {
            outcome = client.update().resource(documentReference).withId(theId).execute();
        } else {
            outcome = client.create().resource(documentReference).execute();
        }
        log.info(outcome.getId().getValue());
        IdType idType = new IdType().setValue(outcome.getId().getValue().replace("?_id=", "/"));
        outcome.setId(idType);
        documentReference.setId(idType);
        outcome.setResource(documentReference);
        log.info(outcome.getId().getValue());

        return outcome;

    }

    Reference getReference(Reference reference) {
        if (reference.getReference().contains("https://directory.spineservices.nhs.uk/STU3/Organization/")) {
            String ods = reference.getReference().replace("https://directory.spineservices.nhs.uk/STU3/Organization/", "");
            reference.setReference("Organization/" + ods);
            reference.getIdentifier().setSystem("https://fhir.nhs.uk/Id/ods-organization-code").setValue(ods);
        }
        return reference;
    }

}
