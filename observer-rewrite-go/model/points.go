package model

import "time"

const (
	TimeDay   = time.Hour * 24
	TimeWeek  = TimeDay * 7
	TimeMonth = TimeWeek * 4
	TimeYear  = TimeMonth * 12
)

type PointMapping struct {
	Points          int   `json:"points"`
	LastInfraction  int64 `json:"lastInfraction"`
	InfractionUntil int64 `json:"infractionUntil"`
}

func (pm *PointMapping) CalculatePunishmentTime() *time.Time {
	result := time.Now()

	if pm.Points < 4 {
		result = result.Add(TimeDay)
	} else if pm.Points < 6 {
		result = result.Add(TimeWeek)
	} else if pm.Points < 8 {
		result = result.Add(TimeWeek * 2)
	} else if pm.Points < 10 {
		result = result.Add(TimeMonth)
	} else if pm.Points < 12 {
		result = result.Add(TimeMonth * 2)
	} else if pm.Points < 14 {
		result = result.Add(TimeMonth * 4)
	} else if pm.Points < 16 {
		result = result.Add(TimeMonth * 8)
	} else {
		return nil
	}

	return &result
}
