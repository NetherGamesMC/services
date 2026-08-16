import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.nethergames.social.server.events.PlayerConnectedEvent;
import org.nethergames.social.server.events.PlayerDisconnectEvent;
import org.nethergames.social.server.manager.LocalityManager;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

public class SubscriberTests {

    @Mock
    private LocalityManager localityService;

    private EventBus eventBus;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        eventBus = new EventBus();
        eventBus.register(localityService);
    }

    @Test
    public void SubscribedConnectedEventsShouldFired() {
        eventBus.post(new PlayerConnectedEvent("138374643612632131", "player1"));

        ArgumentCaptor<PlayerConnectedEvent> captor = ArgumentCaptor.forClass(PlayerConnectedEvent.class);

        verify(localityService, times(1)).onPlayerConnected(captor.capture());

        var event = captor.getValue();
        assertThat(event.getXuid()).isEqualTo("138374643612632131");
        assertThat(event.getPlayerName()).isEqualTo("player1");
    }

    @Test
    public void SubscribedDisconnectedEventsShouldFired() {
        eventBus.post(new PlayerDisconnectEvent("138374643612632131", "player1"));

        ArgumentCaptor<PlayerDisconnectEvent> captor = ArgumentCaptor.forClass(PlayerDisconnectEvent.class);

        verify(localityService, times(1)).onPlayerDisconnected(captor.capture());

        var event = captor.getValue();
        assertThat(event.getXuid()).isEqualTo("138374643612632131");
        assertThat(event.getPlayerName()).isEqualTo("player1");
    }
}
