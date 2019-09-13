package uk.gov.wildfyre.nrl.dao;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.nrl.support.CareConnectConstants;
import uk.gov.wildfyre.nrl.support.NHSDigitalSpineConstants;
import uk.gov.wildfyre.nrl.support.OperationOutcomeException;
import uk.gov.wildfyre.nrl.support.ProviderResponseLibrary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


@Component
public class DocumentReferenceDao implements IDocumentReference {

    private static final Logger log = LoggerFactory.getLogger(DocumentReferenceDao.class);


    @Override
    public DocumentReference read(IGenericClient client, IdType internalId) {

        log.info(internalId.getValue());
        List<DocumentReference> docs = search(client, null, null, null, new TokenParam().setValue(internalId.getIdPart()));
        if (!docs.isEmpty()) {
            return docs.get(0);
        }
        return null;
    }

    @Override
    public MethodOutcome delete(IGenericClient client, IdType internalId)  {
        MethodOutcome method = new MethodOutcome();
        IBaseOperationOutcome resp = client.delete().resourceById(internalId).execute();
        if (resp != null) {
            OperationOutcome outcome = (OperationOutcome) resp;
            method.setOperationOutcome(outcome);
            method.setCreated(false);
        }
        return method;
    }

    @Override
    public MethodOutcome update(IGenericClient client, DocumentReference documentReference, IdType theId, String theConditional)  {
        delete(client,theId);
        MethodOutcome method = new MethodOutcome();
        method.setCreated(true);
        OperationOutcome opOutcome = new OperationOutcome();
        method.setOperationOutcome(opOutcome);

        try {
            method = create(client,documentReference, theId, theConditional);
        } catch (OperationOutcomeException ex) {
            ProviderResponseLibrary.handleException(method,ex);
        }
        return method;
    }

    @Override
    public List<DocumentReference> search(IGenericClient client, ReferenceParam patient, TokenParam type,
                                          ReferenceParam org, TokenParam id)  {

        Bundle result = null;

        IQuery query = null;

        if (patient != null) {
            query = client.search().forResource(DocumentReference.class)
                    .where(DocumentReference.SUBJECT.hasId(getPatientQuery(patient)));
        } else {
            if (id != null) {
                query = client.search().forResource(DocumentReference.class)
                        .where(DocumentReference.RES_ID.exactly().code(id.getValue()));
            } else {
                log.info("patient or _id must be supplied as search parameters");
                return Collections.emptyList();
            }
        }

        if (type != null) {
            query = query.and(DocumentReference.TYPE.exactly().systemAndValues(type.getSystem(), type.getValue()));
        }

        if (org != null) {
            query = query.and(DocumentReference.CUSTODIAN.hasChainedProperty(Organization.IDENTIFIER.exactly().systemAndValues(CareConnectConstants.IdentifierSystem.SDS_ORGANISATION_CODE, getDocumentQuery(org))) );
        }

        try {
            result = (Bundle) query.returnBundle(Bundle.class)
                    .execute();
        } catch (InvalidRequestException invalid) {
            return Collections.emptyList();
        } catch (Exception ex) {
                throw ex;
        }

        return processResult(result);
    }
    private String getDocumentQuery(ReferenceParam org) {
        String documentQry = org.getValue();

        if (!documentQry.contains(NHSDigitalSpineConstants.Url.ORGANISATION)) {
            documentQry = org.getValue();
        } else {
            documentQry = org.getValue().replace(NHSDigitalSpineConstants.Url.ORGANISATION,"");
        }
        if (org.getChain() != null && org.getChain().contains("identifier")) {
            String[] ids = org.getValue().split("|");
            if (ids.length > 1 && ids[0].contains(CareConnectConstants.IdentifierSystem.SDS_ORGANISATION_CODE)) {
                documentQry = ids[1];
            }
        }
        log.info(documentQry);
        return documentQry;
    }

    private String getPatientQuery(ReferenceParam patient) {
        String patientQry = patient.getValue();
        if (!patientQry.contains(NHSDigitalSpineConstants.Url.PATIENT)) {
            patientQry = NHSDigitalSpineConstants.Url.PATIENT + patient.getValue();
        }
        if (patient.getChain() != null && patient.getChain().contains("identifier")) {
            String[] ids = patient.getValue().split("|");
            if (ids.length > 1 && ids[0].contains(CareConnectConstants.IdentifierSystem.NHS_NUMBER
            )) {
                patientQry = NHSDigitalSpineConstants.Url.PATIENT + ids[1];
            }
        }
        log.trace(patientQry);
        return patientQry;
    }

    private List<DocumentReference> processResult(Bundle result) {

        List<DocumentReference> documents = new ArrayList<>();
        if (result != null && !result.getEntry().isEmpty()) {
            for (Bundle.BundleEntryComponent entry : result.getEntry()) {
                if (entry.getResource() instanceof DocumentReference) {
                    DocumentReference documentReference = (DocumentReference) entry.getResource();
                    processResultSubject(documentReference);
                    processResultAuthor(documentReference);
                    if (documentReference.hasCustodian()) {

                        documentReference.setCustodian(getReference(documentReference.getCustodian()));
                    }
                    documents.add(documentReference);
                }
            }
        }
        return documents;
    }

    private void processResultAuthor(DocumentReference documentReference) {
        if (documentReference.hasAuthor()) {
            List<Reference> refs = new ArrayList<>();
            for (Reference ref : documentReference.getAuthor()) {
                refs.add(getReference(ref));
            }
            documentReference.setAuthor(refs);
        }

    }
    private void processResultSubject(DocumentReference documentReference) {
        if (documentReference.hasSubject() && documentReference.getSubject().getReference().contains(NHSDigitalSpineConstants.Url.PATIENT)) {
            String nhsNumber = documentReference.getSubject().getReference().replace(NHSDigitalSpineConstants.Url.PATIENT, "");
            Reference ref = new Reference("Patient/" + nhsNumber);
            ref.getIdentifier().setSystem(CareConnectConstants.IdentifierSystem.NHS_NUMBER).setValue(nhsNumber);
            documentReference.setSubject(ref);
        }
    }
    @Override
    public MethodOutcome create(IGenericClient client, DocumentReference originalDocumentReference, IdType theId, String theConditional) throws OperationOutcomeException {

        DocumentReference documentReference = originalDocumentReference;
        if (theId == null) {
            documentReference.setId("");
        } else {
            documentReference.setId(theId.getValue());
        }
        documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        processSubject(documentReference);

        processAuthor(documentReference);

        documentReference.setIndexed(new Date());

        processCustodian(documentReference);


        documentReference.setType(null);

        documentReference.getType().addCoding()
                .setSystem("http://snomed.info/sct")
                .setDisplay("Mental health crisis plan")
                .setCode("736253002");
        documentReference.setContext(null);

        if (!documentReference.hasStatus()) {
            documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        }
        CodeableConcept codeClass = new CodeableConcept();
        codeClass.addCoding()
                .setCode("734163000")
                .setSystem("http://snomed.info/sct")
                .setDisplay("Care plan");
        documentReference.setClass_(codeClass);

        DocumentReference.DocumentReferenceContentComponent content = documentReference.getContentFirstRep();
        if (!content.hasFormat()) {
            content.setFormat(new Coding()
                    .setCode("proxy:https://www.iso.org/standard/63534.html")
                    .setDisplay("PDF")
                    .setSystem("https://fhir.nhs.uk/STU3/CodeSystem/NRL-FormatCode-1"));
        }

        if (!content.hasExtension()) {
            Extension extension = content.addExtension();
            extension.setUrl("https://fhir.nhs.uk/STU3/StructureDefinition/Extension-NRL-ContentStability-1");
            CodeableConcept codeableConcept = new CodeableConcept();
            codeableConcept.addCoding()
                    .setSystem("https://fhir.nhs.uk/STU3/CodeSystem/NRL-ContentStability-1")
                    .setCode("static")
                    .setDisplay("Static");
            extension.setValue(codeableConcept);
        }

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

    private void processAuthor(DocumentReference documentReference) {
        if (documentReference.hasAuthor()) {
            Reference masterRef = processAuthorReferences(documentReference);
            if (!masterRef.hasReference() || !masterRef.getReference().contains(NHSDigitalSpineConstants.Url.ORGANISATION)) {
                throw new UnprocessableEntityException("Invalid Author reference");
            } else {
                documentReference.setAuthor(new ArrayList<>());
                documentReference.getAuthor().add(masterRef);
            }
        } else {
            throw new UnprocessableEntityException("Author must be present");
        }
    }
    private Reference processAuthorReferences(DocumentReference documentReference) {
        Reference masterRef = new Reference();

        for (Reference ref : documentReference.getAuthor()) {
            if (ref.hasReference()) {
                if (ref.getReference().startsWith("Organization")) {
                    masterRef.setReference(NHSDigitalSpineConstants.Url.BASE + ref);
                } else {
                    masterRef.setReference(ref.getReference());
                }
            }
            if (ref.hasIdentifier() && ref.getIdentifier().getSystem().equals(CareConnectConstants.IdentifierSystem.SDS_ORGANISATION_CODE)) {
                masterRef.setReference(NHSDigitalSpineConstants.Url.ORGANISATION + ref.getIdentifier().getValue());
            }
        }
        return masterRef;
    }

    private void processCustodian(DocumentReference documentReference) {
        if (documentReference.hasCustodian()) {
            Reference ref = documentReference.getCustodian();

            if (ref.hasReference() && ref.getReference().startsWith("Organization")) {
                ref.setReference(NHSDigitalSpineConstants.Url.BASE + ref);
            }
            if (ref.hasIdentifier() && ref.getIdentifier().getSystem().equals(CareConnectConstants.IdentifierSystem.SDS_ORGANISATION_CODE)) {
                ref.setReference(NHSDigitalSpineConstants.Url.ORGANISATION + ref.getIdentifier().getValue());

            }
            log.info(ref.getReference());
            if (!ref.hasReference() || !ref.getReference().contains(NHSDigitalSpineConstants.Url.ORGANISATION)) {
                throw new UnprocessableEntityException("Invalid Custodian reference");
            } else {
                ref.setIdentifier(null);
            }
        } else {
            throw new UnprocessableEntityException("Custodian must be present");
        }
    }
    private void processSubject(DocumentReference documentReference) {
        if (documentReference.hasSubject()) {
            if (documentReference.getSubject().hasReference()) {
                if (documentReference.getSubject().getReference().startsWith("Patient")) {
                    documentReference.getSubject().setReference(NHSDigitalSpineConstants.Url.BASE + documentReference.getSubject().getReference());
                }
                if (!documentReference.getSubject().getReference().contains(NHSDigitalSpineConstants.Url.PATIENT)) {
                    throw new UnprocessableEntityException("Invalid subject reference");
                }
            }
            if (documentReference.getSubject().hasIdentifier() && documentReference.getSubject().getIdentifier().getSystem().equals(CareConnectConstants.IdentifierSystem.NHS_NUMBER)) {
                documentReference.setSubject(new Reference(NHSDigitalSpineConstants.Url.PATIENT + documentReference.getSubject().getIdentifier().getValue()));
            }

        } else {
            throw new UnprocessableEntityException("Subject must be present");
        }

    }

    Reference getReference(Reference reference) {
        if (reference.getReference().contains(NHSDigitalSpineConstants.Url.ORGANISATION)) {
            String ods = reference.getReference().replace(NHSDigitalSpineConstants.Url.ORGANISATION, "");
            reference.setReference("Organization/" + ods);
            reference.getIdentifier().setSystem(CareConnectConstants.IdentifierSystem.SDS_ORGANISATION_CODE).setValue(ods);
        }
        return reference;
    }

}
