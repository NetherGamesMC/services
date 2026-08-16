package model

import (
	"slices"
	"time"
)

type ReportResolution string

const (
	ReportResolutionPunished     ReportResolution = "PUNISHED"
	ReportResolutionInsufficient ReportResolution = "INSUFFICIENT"
	ReportRes
)

var AllReportResolutionValues = []ReportResolution{ReportResolutionPunished, ReportResolutionInsufficient}

type PlayerReport struct {
	Player          string         `json:"player"`
	LastReported    int64          `json:"lastReported"`
	PlayersReported []string       `json:"playersReported"`
	MatchesReported []string       `json:"matchesReported"`
	ReportHits      map[string]int `json:"reportHit"`
	TraineeClaimed  string         `json:"traineeClaimed"`
	TotalReports    int            `json:"totalReports"`
}

type PlayerReportData struct {
	Player         string  `json:"player"`
	Reporter       string  `json:"reporter"`
	Reason         string  `json:"reportReason"`
	ServerLocation string  `json:"serverLocation"`
	ReplayID       *string `json:"replayId"`
}

func (report *PlayerReport) AddReport(reason string, replayId *string) {
	report.LastReported = time.Now().Unix()
	hit, ok := report.ReportHits[reason]

	if !ok {
		hit = 0
	}

	report.ReportHits[reason] = hit + 1
	if replayId != nil && !slices.Contains(report.MatchesReported, *replayId) {
		report.MatchesReported = append(report.MatchesReported, *replayId)
	}

	report.TotalReports += 1
}
