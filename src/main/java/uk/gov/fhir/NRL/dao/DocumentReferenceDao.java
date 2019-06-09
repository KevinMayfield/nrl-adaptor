package uk.gov.fhir.NRL.dao;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class DocumentReferenceDao implements IDocumentReference {

    private static final Logger log = LoggerFactory.getLogger(DocumentReferenceDao.class);



    @Override
    public DocumentReference read(IGenericClient client, IdType internalId) {

        Bundle result = client.search()
                .forResource(Patient.class)
                .where(new TokenClientParam("identifier").exactly().systemAndCode("https://fhir.nhs.uk/Id/nhs-number", internalId.getIdPart()))
                .returnBundle(Bundle.class)
                .execute();


        return null;
    }

    @Override
    public List<DocumentReference> search(IGenericClient client, ReferenceParam patient) throws Exception {


        List<DocumentReference> documents = new ArrayList<>();
        Bundle result= null;

        try {
            result = client.search().forResource(DocumentReference.class)
                    .where(DocumentReference.SUBJECT.hasId("https://demographics.spineservices.nhs.uk/STU3/Patient/"+patient.getValue()))
                    .returnBundle(Bundle.class)
                    .execute();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }

        if (result != null && result.getEntry().size()>0) {
            for (Bundle.BundleEntryComponent entry : result.getEntry()) {
                if (entry.getResource() instanceof DocumentReference) {
                    DocumentReference documentReference = (DocumentReference) entry.getResource();
                    if (documentReference.hasSubject()) {
                        if (documentReference.getSubject().getReference().contains("https://demographics.spineservices.nhs.uk/STU3/Patient/")) {
                            String nhsNumber = documentReference.getSubject().getReference().replace("https://demographics.spineservices.nhs.uk/STU3/Patient/","");
                            Reference ref = new Reference("Patient/"+nhsNumber);
                            ref.getIdentifier().setSystem("https://fhir.nhs.uk/Id/nhs-number").setValue(nhsNumber);
                            documentReference.setSubject(ref);
                        }
                    }
                    if (documentReference.hasAuthor()) {
                        List<Reference> refs = new ArrayList<>();
                        for ( Reference ref : documentReference.getAuthor()) {
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

    Reference getReference(Reference reference) {
        if (reference.getReference().contains("https://directory.spineservices.nhs.uk/STU3/Organization/")) {
            String ods = reference.getReference().replace("https://directory.spineservices.nhs.uk/STU3/Organization/","");
            reference.setReference("Organization/"+ods);
            reference.getIdentifier().setSystem("https://fhir.nhs.uk/Id/ods-organization-code").setValue(ods);
        }
        return reference;
    }

}
