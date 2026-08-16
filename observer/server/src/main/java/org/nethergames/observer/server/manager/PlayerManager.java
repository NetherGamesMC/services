package org.nethergames.observer.server.manager;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.nethergames.observer.data.punishment.PlayerComment;
import org.nethergames.observer.server.Observer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PlayerManager {
    public void addComment(PlayerComment comment) {
        comment.setPublishedAt(Instant.now().getEpochSecond());
        Observer.getObserver().getMongoManager().getCommentCollections().insertOne(comment);
    }

    public List<PlayerComment> getCommentsFor(String xuid) {
        FindIterable<PlayerComment> comments = Observer.getObserver().getMongoManager().getCommentCollections().find(
                Filters.eq("xuid", xuid)
        );

        comments = comments.sort(Sorts.descending("publishedAt"));
        ArrayList<PlayerComment> list = new ArrayList<>();
        comments.forEach(list::add);

        return list;
    }
}
