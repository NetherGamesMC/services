package main

import (
	"Observer/model"
	"Observer/service"
	"github.com/stretchr/testify/assert"
	"testing"
	"time"
)

func TestPointMappingsNoData(t *testing.T) {
	result := service.CalculatePunishmentPoints([]model.Punishment{})

	if result.Points != 0 {
		t.Fail()
	}
}

func TestPointMappings1Punishment(t *testing.T) {
	assert := assert.New(t)

	punishments := []model.Punishment{{
		Permanent:  false,
		ValidUntil: time.Now().Add(-model.TimeMonth).Unix(),
		IssuedAt:   time.Now().Add(-(model.TimeMonth * 3)).Unix(),
		Reason: model.PunishmentReason{
			Points: 10,
		},
	}}

	result := service.CalculatePunishmentPoints(punishments)

	assert.Equal(9, result.Points, "Points must be correct")
}

// Following scenario
// Player gets first ban 1 Year 1 Month 5 Days ago
// Ban lasts until 1 Year 5 days ago (so one month), worth 10 points
// Player gets next ban 3 months ago
// Between these two bans, 9 full months (rounded up) pass, so player gets a point reduction of 9 points (= 1 in total)
// player then gets 5 points on top, so 6 in total
// punishment ends 1 month 5 days ago, which rounded is 2 months, gets 2 points removed, ends up at four
func TestPointMappings2Punishments(t *testing.T) {
	assert := assert.New(t)

	punishments := []model.Punishment{
		{
			Permanent:  false,
			ValidUntil: time.Now().Add(-(model.TimeYear + 5*model.TimeDay)).Unix(),
			IssuedAt:   time.Now().Add(-(model.TimeYear + model.TimeMonth + 5*model.TimeDay)).Unix(),
			Reason: model.PunishmentReason{
				Points: 10,
			},
		},
		{
			Permanent:  false,
			ValidUntil: time.Now().Add(-(model.TimeMonth + 10*model.TimeDay)).Unix(),
			IssuedAt:   time.Now().Add(-(model.TimeMonth * 3)).Unix(),
			Reason: model.PunishmentReason{
				Points: 5,
			},
		},
	}

	result := service.CalculatePunishmentPoints(punishments)

	assert.Equal(4, result.Points, "Points must be correct")

}
