package service

import (
	"Observer/db"
	"Observer/event_broadcast"
	"Observer/model"
	"errors"
	"slices"
	"time"
)

func CreateReport(player string, reporter string, reason string, location string, replayId *string) (*model.PlayerReport, error) {
	existingReport, err := db.Provider.GetReport(player)

	if err != nil {
		return nil, err
	}

	if existingReport == nil {
		existingReport = &model.PlayerReport{
			Player:          player,
			LastReported:    time.Now().Unix(),
			PlayersReported: []string{reporter},
			TotalReports:    0,
			ReportHits:      make(map[string]int),
		}

		if replayId != nil {
			existingReport.MatchesReported = []string{*replayId}
		}
	} else {
		if slices.Contains(existingReport.PlayersReported, reporter) {
			return nil, errors.New("that player already reported the given player")
		}

		existingReport.PlayersReported = append(existingReport.PlayersReported, reporter)
	}

	existingReport.AddReport(reason, replayId)

	err = db.Provider.UpsertReport(existingReport)

	if err != nil {
		return nil, err
	}

	err = event_broadcast.Provider.BroadcastReportUpserted(existingReport)

	if err != nil { // todo: this is only broadcasting. We might allow this to silently fail for the user
		return nil, err
	}

	return existingReport, nil
}
