package event_broadcast

import (
	"Observer/event_broadcast/kafka"
	"Observer/model"
)

var Provider EventBroadcastProvider = &kafka.KafkaBroadcastProvider{}

type EventBroadcastProvider interface {
	Init() error
	BroadcastReportUpserted(report *model.PlayerReport) error
	BroadcastReportProcessed(report *model.PlayerReport, resolution model.ReportResolution) error

	BroadcastPunishmentAdded(punishment *model.Punishment) error
	BroadcastPunishmentRemoved(punishment *model.Punishment) error
	BroadcastPunishmentUpdated(punishment *model.Punishment) error
}
