package org.nethergames.observer.server.manager;

import com.mongodb.client.MongoCollection;
import com.mongodb.lang.Nullable;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import lombok.extern.log4j.Log4j2;
import org.nethergames.observer.data.evidences.PunishmentEvidence;
import org.nethergames.observer.data.evidences.type.EvidenceType;
import org.nethergames.observer.data.punishment.Punishment;
import org.nethergames.observer.server.Observer;
import org.nethergames.observer.server.exception.EvidenceNotFoundException;
import org.nethergames.observer.server.exception.PunishmentNotFoundException;
import org.nethergames.observer.server.exception.UploadLimitException;
import org.nethergames.observer.server.generator.Snowflake;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

@Log4j2(topic = "EvidenceManager")
public class EvidenceManager {

    private static final int MAXIMUM_EVIDENCES_UPLOAD = 10;

    private final Observer observer;
    private final Snowflake snowflake;
    private final KeyLockManager lockManager;

    public EvidenceManager(Observer observer) {
        this.observer = observer;
        this.snowflake = new Snowflake();
        this.lockManager = KeyLockManagers.newLock(20, TimeUnit.SECONDS);

        MongoManager.startMigration();
    }

    public List<PunishmentEvidence> getEvidences(String id) {
        List<PunishmentEvidence> evidences = new ArrayList<>();
        MongoCollection<PunishmentEvidence> collections = Observer.getObserver().getMongoManager().getEvidenceCollections();

        collections.find(eq("punishmentId", id)).forEach(evidence -> {
            if (evidence.getType() == EvidenceType.AWS_MANAGED) {
                evidence.setData(observer.getStorageProvider().getObjectUrl(evidence.getData()));
            }

            evidences.add(evidence);
        });

        return evidences;
    }

    public PunishmentEvidence addEvidence(String punishmentId, PunishmentEvidence data) {
        return lockManager.executeLocked(punishmentId, () -> {
            var punishment = MongoManager.getPunishment(punishmentId);
            if (punishment == null) {
                throw new PunishmentNotFoundException(punishmentId);
            }

            var evidences = getEvidences(punishmentId);
            if (evidences.size() >= MAXIMUM_EVIDENCES_UPLOAD) {
                throw new UploadLimitException();
            }

            var evidence = new PunishmentEvidence(punishment.getId(), snowflake.nextId(), punishment.getXuid(), data.getAttachedBy(), data.getType(), data.getData(), data.getNote());

            MongoManager.addEvidence(evidence);

            return evidence;
        });
    }

    public PunishmentEvidence addEvidence(String punishmentId, String staff, String contentType, InputStream stream) {
        return lockManager.executeLocked(punishmentId, () -> {
            var punishment = MongoManager.getPunishment(punishmentId);
            if (punishment == null) {
                throw new PunishmentNotFoundException(punishmentId);
            }

            var evidences = getEvidences(punishmentId);
            if (evidences.size() >= MAXIMUM_EVIDENCES_UPLOAD) {
                throw new UploadLimitException();
            }

            var id = snowflake.nextId();
            var fileName = String.format("%s/%s-%s", punishment.getXuid(), punishment.getId(), id);
            var evidence = new PunishmentEvidence(punishment.getId(), id, punishment.getXuid(), staff, EvidenceType.AWS_MANAGED, fileName, "");

            observer.getStorageProvider().uploadStreamMultipart(fileName, contentType, stream);

            MongoManager.addEvidence(evidence);

            return evidence;
        });
    }

    public void removeEvidence(String punishmentId, long evidenceId) {
        var evidences = getEvidences(punishmentId);
        var evidenceResult = evidences.stream().filter(ev -> ev.getEvidenceId() == evidenceId).findFirst();
        if (evidenceResult.isEmpty()) {
            throw new EvidenceNotFoundException(punishmentId);
        }

        var evidence = evidenceResult.get();
        if (evidence.getType() == EvidenceType.AWS_MANAGED) {
            observer.getStorageProvider().deleteObject(evidence.getData());
        }

        if (!MongoManager.deleteEvidence(evidence)) {
            throw new EvidenceNotFoundException(punishmentId);
        }
    }

    public PunishmentEvidence updateEvidenceNote(Punishment punishment, Long evidenceId, @Nullable String note) {
        var evidence = punishment.getEvidence(evidenceId);
        if (evidence == null) {
            throw new EvidenceNotFoundException(Long.toString(evidenceId));
        }

        evidence.setNote(note);

        MongoManager.updatePunishment(punishment);

        return evidence;
    }
}
