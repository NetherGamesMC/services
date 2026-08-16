package kafka

import (
	"Observer/model"
	"encoding/json"
)

func (kbp *KafkaBroadcastProvider) BroadcastPunishmentAdded(punishment *model.Punishment) error {
	encoded, err := json.Marshal(punishment)
	if err != nil {
		return err
	}

	return kbp.broadcastMessage("observer", "punished", encoded)
}

func (kbp *KafkaBroadcastProvider) BroadcastPunishmentRemoved(punishment *model.Punishment) error {
	encoded, err := json.Marshal(punishment)
	if err != nil {
		return err
	}

	return kbp.broadcastMessage("observer", "removal", encoded)
}

func (kbp *KafkaBroadcastProvider) BroadcastPunishmentUpdated(punishment *model.Punishment) error {
	encoded, err := json.Marshal(punishment)
	if err != nil {
		return err
	}

	return kbp.broadcastMessage("observer", "update", encoded)
}
