package db

import (
	"Observer/db/options"
	"Observer/db/provider/mongo"
	"Observer/model"
)

var Provider DataProvider = &mongo.MongoDataProvider{}

type DataProvider interface {
	Start() error
	UpsertPunishment(punishment model.Punishment) error
	DeletePunishment(punishment model.Punishment) error
	DeletePunishmentsByReason(reason model.PunishmentReason) error

	GetPunishmentById(id string) (*model.Punishment, error)
	GetPunishments(XboxIDs string, options options.GetPunishmentOptions) (*[]model.Punishment, error)
	GetPunishmentsMultiple(XboxIDs []string, options options.GetPunishmentOptions) (*[]model.Punishment, error)

	DoTrace(XboxID string, options options.DoTraceOptions) (*model.TracingSearchResult, error)
	UpsertPointMapping(mapping model.PointMapping) error

	UpsertReport(report *model.PlayerReport) error
	GetReport(player string) (*model.PlayerReport, error)
	GetReports(players []string) (*[]model.PlayerReport, error)

	GetReason(name string) (*model.PunishmentReason, error)
	UpsertReason(reason model.PunishmentReason, opts options.PatchReasonOptions) error
	DeleteReason(reason model.PunishmentReason, opts options.DeleteReasonOptions) error
	GetReasons() (*[]model.PunishmentReason, error)
}
