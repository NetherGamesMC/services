package org.nethergames.observer.server.controller;

import io.javalin.http.Context;
import io.javalin.openapi.*;
import org.nethergames.observer.data.evidences.PunishmentEvidence;
import org.nethergames.observer.data.evidences.TemporaryEvidence;
import org.nethergames.observer.data.evidences.type.EvidenceType;
import org.nethergames.observer.data.punishment.Punishment;
import org.nethergames.observer.server.Observer;
import org.nethergames.observer.server.exception.EvidenceNotFoundException;
import org.nethergames.observer.server.exception.ParseErrorException;
import org.nethergames.observer.server.exception.PunishmentNotFoundException;
import org.nethergames.observer.server.exception.UploadIllegalException;
import org.nethergames.observer.server.manager.MongoManager;

import java.io.IOException;

public class EvidenceController {

    // PUT /punishment/{id}/evidence: Set/replaces the current punishment evidence.
    // Content: The content are the evidence itself.
    // Parameter (In headers):
    // - Upload-Issuer:         The uploader that uploads the evidence.
    // - Content-Type:          The uploaded file content type.

    @OpenApi(
            summary = "Upload an evidence to the S3 storage server.",
            path = "/punishment/{id}/evidence",
            methods = HttpMethod.PUT,
            tags = "Evidences",
            pathParams = {
                    @OpenApiParam(name = "id", required = true, description = "The punishment id", example = "0109b06caf")
            },
            headers = {
                    @OpenApiParam(name = "Upload-Issuer", description = "An xuid of the uploader who uploaded the evidence to the Observer (Required only if binary data is being uploaded)."),
                    @OpenApiParam(name = "Content-Type", required = true, description = "HTTP header that specifies the media type of the resource being sent to the Observer.")
            },
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = {
                            @OpenApiContent(from = Byte[].class, mimeType = "application/octet-stream"),
                            @OpenApiContent(from = PunishmentEvidence.class, mimeType = "application/json")
                    }
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = PunishmentEvidence.class, mimeType = "application/json"), description = "The evidence was uploaded without any issues."),
                    @OpenApiResponse(status = "500", description = "")
            }
    )
    public static void uploadEvidence(Context context) {
        var punishmentId = context.pathParam("id");
        var contentType = context.contentType();

        if (contentType == null) {
            throw new ParseErrorException("Content-Type headers are required");
        }

        PunishmentEvidence evidenceId;

        // If the contentType is application/json, treat it as a PunishmentEvidence object.
        if (contentType.equalsIgnoreCase("application/json")) {
            var evidence = context.bodyAsClass(PunishmentEvidence.class);

            if (evidence.getType() == EvidenceType.AWS_MANAGED) {
                throw new UploadIllegalException();
            }

            evidenceId = Observer.getObserver().getEvidenceManager().addEvidence(punishmentId, evidence);
        } else {
            var uploadIssuer = context.header("Upload-Issuer");

            if (uploadIssuer == null) {
                throw new ParseErrorException("Upload-Issuer header are required");
            }

            evidenceId = Observer.getObserver().getEvidenceManager().addEvidence(punishmentId, uploadIssuer, context.contentType(), context.bodyInputStream());
        }

        context.json(evidenceId);
    }

    // PATCH /punishment/{id}/evidence: Patch the evidence note (In case a staff wanted to add something to it).
    // The body itself is the note for the given evidence.

    @OpenApi(
            summary = "Update a note of a given evidence id",
            path = "/punishment/{id}/evidence",
            methods = HttpMethod.PATCH,
            tags = "Evidences",
            pathParams = {
                    @OpenApiParam(name = "id", required = true, description = "The punishment id", example = "0109b06caf")
            },
            queryParams = {
                    @OpenApiParam(name = "evidenceId", required = true, description = "The evidence id", example = "10119875177239370")
            },
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = @OpenApiContent(from = String.class, mimeType = "application/json")
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = PunishmentEvidence.class), description = "The updated evidence of a given punishment id."),
                    @OpenApiResponse(status = "404", content = @OpenApiContent(from = PunishmentNotFoundException.class), description = "The given punishment id was not found.")
            }
    )
    public static void updateEvidenceNote(Context context) {
        var punishmentId = context.pathParam("id");
        var evidenceId = context.queryParamAsClass("evidenceId", Long.class).getOrThrow((i) -> new EvidenceNotFoundException(""));
        var note = context.bodyAsClass(String.class);

        var punishment = MongoManager.getPunishment(punishmentId);
        if (punishment == null) {
            throw new PunishmentNotFoundException(punishmentId);
        }

        context.json(Observer.getObserver().getEvidenceManager().updateEvidenceNote(punishment, evidenceId, note));
    }

    // GET /punishment/{id}/evidence: Get the evidence to the given punishment.
    // This method will return a PunishmentEvidence object, the object would be parsed if an AWS object
    // were to return.

    @OpenApi(
            summary = "Get the evidence context of a given punishment id.",
            path = "/punishment/{id}/evidence",
            methods = HttpMethod.GET,
            tags = "Evidences",
            pathParams = {
                    @OpenApiParam(name = "id", required = true, description = "The punishment id", example = "0109b06caf")
            },
            queryParams = {
                    @OpenApiParam(name = "evidenceId", description = "The evidence id", example = "10119875177239370")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = PunishmentEvidence.class), description = "The evidence of a given punishment id. A temporary link will be generated if an AWS managed storage is present, the link will expire in 60 minutes."),
                    @OpenApiResponse(status = "404", content = @OpenApiContent(from = PunishmentNotFoundException.class), description = "The given punishment id was not found.")
            }
    )
    public static void getEvidence(Context context) {
        var punishmentId = context.pathParam("id");
        var evidenceId = context.queryParamAsClass("evidenceId", Long.class).getOrDefault(null);

        var punishment = MongoManager.getPunishment(punishmentId);
        if (punishment == null) {
            throw new PunishmentNotFoundException(punishmentId);
        }

        var evidences = Observer.getObserver().getEvidenceManager().getEvidences(punishment.getId());
        if (evidenceId == null) {
            context.json(evidences);
        } else {
            var evidence = evidences.stream().filter(i -> i.getEvidenceId() == evidenceId).findFirst().orElse(null);

            if (evidence == null) {
                throw new EvidenceNotFoundException(punishmentId);
            } else {
                context.json(evidence);
            }
        }
    }

    @OpenApi(
            summary = "Delete the given evidence for a punishment id.",
            path = "/punishment/{id}/evidence",
            methods = HttpMethod.DELETE,
            tags = "Evidences",
            pathParams = {
                    @OpenApiParam(name = "id", required = true, description = "The punishment id", example = "0109b06caf")
            },
            queryParams = {
                    @OpenApiParam(name = "evidenceId", required = true, description = "The evidence id", example = "10119875177239370")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "The evidence was removed without any issues."),
                    @OpenApiResponse(status = "404", content = @OpenApiContent(from = PunishmentNotFoundException.class), description = "The given punishment id was not found.")
            }
    )
    public static void deleteEvidence(Context context) {
        String punishmentId = context.pathParam("id");
        long evidenceId = context.queryParamAsClass("evidenceId", Long.class).getOrThrow((i) -> new EvidenceNotFoundException(""));

        Observer.getObserver().getEvidenceManager().removeEvidence(punishmentId, evidenceId);

        context.status(200);
    }

    // PUT /evidence/{report_id}: Upload an evidence into temporary bucket.
    // Parameter (In headers):
    // - Content-Type:          The uploaded file content type.

    @OpenApi(
            summary = "Upload an evidence to the temporary bucket storage.",
            path = "/evidence/{report_id}",
            methods = HttpMethod.PUT,
            tags = "Evidences",
            pathParams = {
                    @OpenApiParam(name = "report_id", required = true, description = "The identifier to the evidence in the temporary bucket storage.", example = "1614366567833796608")
            },
            headers = {
                    @OpenApiParam(name = "Content-Type", required = true, description = "HTTP header that specifies the media type of the resource being sent to the Observer.")
            },
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = @OpenApiContent(from = Byte[].class)
            ),
            responses = {
                    @OpenApiResponse(status = "200", description = "The evidence was uploaded without any issues.")
            }
    )
    public static void uploadTemporaryEvidence(Context context) {
        var reportId = context.pathParam("report_id");

        var storageProvider = Observer.getObserver().getAltStorageProvider();

        storageProvider.uploadStreamMultipart(reportId, context.contentType(), context.bodyInputStream());

        context.status(200);
    }

    // GET /evidence/{report-id}/{file-name}: Redirect the given report id to the given evidence attached.
    // Evidence attached will expire in 60 minutes.

    @OpenApi(
            summary = "Return the evidence found in the temporary evidence bucket.",
            path = "/evidence/{report_id}",
            methods = HttpMethod.GET,
            tags = "Evidences",
            pathParams = {
                    @OpenApiParam(name = "report_id", required = true, description = "The identifier to the evidence in the temporary bucket storage.", example = "1614366567833796608")
            },
            responses = {
                    @OpenApiResponse(status = "302", description = "Redirect to the evidence found in the temporary evidence bucket."),
                    @OpenApiResponse(status = "404", content = @OpenApiContent(from = EvidenceNotFoundException.class), description = "The given report id was not found.")
            }
    )
    public static void redirectTemporaryEvidence(Context context) {
        var reportId = context.pathParam("report_id");

        var storageProvider = Observer.getObserver().getAltStorageProvider();
        if (!storageProvider.isObjectExists(reportId)) {
            throw new EvidenceNotFoundException(reportId);
        }

        context.redirect(storageProvider.getObjectUrl(reportId));
    }

    // PATCH /evidence/{report_id}: Patch an existing evidence.
    // The body content contains data in which it indicates if an evidence uploaded is accepted or not.
    // Example: {"accepted": false} or {"accepted": true, "punishment_id": "09ca6fc679", "issuer_id": "2535439601173645"}

    @OpenApi(
            summary = "Promote a temporary object into a permanent bucket object storage or delete the temporary object.",
            path = "/evidence/{report_id}",
            methods = HttpMethod.POST,
            tags = "Evidences",
            pathParams = {
                    @OpenApiParam(name = "report_id", required = true, description = "The identifier to the evidence in the temporary bucket storage.", example = "1614366567833796608")
            },
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = @OpenApiContent(from = TemporaryEvidence.class)
            ),
            responses = {
                    @OpenApiResponse(status = "302", description = "The evidence resource that was found"),
                    @OpenApiResponse(status = "404", content = @OpenApiContent(from = EvidenceNotFoundException.class), description = "The given report id was not found.")
            }
    )
    public static void patchTemporaryEvidence(Context context) {
        var reportId = context.pathParam("report_id");
        var object = context.bodyAsClass(TemporaryEvidence.class);

        var storage = Observer.getObserver().getAltStorageProvider();
        if (!storage.isObjectExists(reportId)) {
            throw new EvidenceNotFoundException(reportId);
        }

        if (!object.isAccepted()) {
            storage.deleteObject(reportId);
        } else {
            // Verify if punishment ID exists.
            Punishment punishment = MongoManager.getPunishment(object.getPunishmentId());
            if (punishment == null) {
                context.status(404);
                return;
            }

            // Channel the object stream to the other evidence handler.
            try (var stream = storage.getObjectStream(reportId)) {
                Observer.getObserver().getEvidenceManager().addEvidence(punishment.getId(), object.getIssuerId(), stream.response().contentType(), stream);

                storage.deleteObject(reportId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
