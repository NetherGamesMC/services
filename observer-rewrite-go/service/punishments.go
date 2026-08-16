package service

import (
	"Observer/db"
	"Observer/db/options"
	"Observer/event_broadcast"
	"Observer/model"
	"crypto/rand"
	"errors"
	"fmt"
	"github.com/sirupsen/logrus"
	"math"
	"slices"
	"time"
)

func CreatePlayerPunishment(XboxID string, reason model.PunishmentReason, issuer string, note string) (*model.Punishment, error) {
	existingPunishments, err := db.Provider.GetPunishments(XboxID, options.GetPunishmentOptions{
		FilterTypes: []model.PunishmentType{reason.Type},
	})

	if err != nil {
		return nil, err
	}

	var categoryPunishments []model.Punishment

	if len(*existingPunishments) > 0 { // make sure the player is not already banned or muted, depending on what action the requester wants to take
		for _, existingPunishment := range *existingPunishments {
			if existingPunishment.IsActive() {
				return nil, errors.New("player is already punished")
			}

			if existingPunishment.Reason.Category == reason.Category {
				categoryPunishments = append(categoryPunishments, existingPunishment)
			}
		}
	}

	// Calculate point mapping
	pointMapping := CalculatePunishmentPoints(categoryPunishments)
	pointMapping.Points += reason.Points

	punishmentTime := pointMapping.CalculatePunishmentTime()
	punishment := model.Punishment{
		ID:       randomString(10),
		XboxID:   XboxID,
		Reason:   reason,
		IssuedAt: time.Now().Unix(),
		IssuedBy: issuer,
		Note:     note,
	}

	if punishmentTime == nil {
		punishment.Permanent = true
	} else {
		punishment.ValidUntil = (*punishmentTime).Unix()
	}

	err = db.Provider.UpsertPunishment(punishment)
	if err != nil {
		return nil, err
	}

	// todo same thing as before: Do we really want to tell the user that the entire request failed simply because broadcasting did?
	// todo Either we broadcast and punish or neither, otherwise things might get out of sync. Maybe rollback transaction?
	err = event_broadcast.Provider.BroadcastPunishmentAdded(&punishment)

	if err != nil {
		return nil, err
	}

	return &punishment, err
}

func randomString(length int) string {
	b := make([]byte, length+2)
	rand.Read(b)
	return fmt.Sprintf("%x", b)[2 : length+2]
}

func CalculatePunishmentPoints(punishments []model.Punishment) model.PointMapping {
	slices.SortFunc(punishments, func(A model.Punishment, B model.Punishment) int {
		return int(A.IssuedAt - B.IssuedAt) // Negative if A < B, positive if A > B.
	})

	result := model.PointMapping{}

	var lastPunishmentEndDate *time.Time
	lastPunishmentEndDate = nil

	var lastPunishment *model.Punishment

	for _, punishment := range punishments {
		result.Points += punishment.Reason.Points

		if result.Points > 16 {
			result.Points = 16
		}

		if punishment.Permanent {
			continue
		}

		if lastPunishmentEndDate != nil {
			issuedAt := time.Unix(punishment.IssuedAt, 0)
			diff := issuedAt.Sub(*lastPunishmentEndDate)
			months := math.Ceil(diff.Hours() / 24 / 30)

			logrus.Infof("Reducing points by %v because of time difference between endOf=%v and startOf=%v", int(months), lastPunishmentEndDate.Format("01-02-2006 15:04:05"), issuedAt.Format("01-02-2006 15:04:05"))

			result.Points = max(result.Points-int(months), 0)
		}

		newLast := time.Unix(punishment.ValidUntil, 0)

		lastPunishmentEndDate = &newLast

		result.LastInfraction = punishment.IssuedAt
		result.InfractionUntil = punishment.ValidUntil

		lastPunishment = &punishment
	}

	// This is for reducing the points for the time that has passed from the last punishment until now
	if lastPunishment != nil && !lastPunishment.Permanent {
		validUntil := time.Unix(lastPunishment.ValidUntil, 0)

		if validUntil.Before(time.Now()) {
			diff := time.Now().Sub(validUntil)
			months := math.Ceil(diff.Hours() / 24 / 30)
			logrus.Infof("Reducing points by %v because of after time difference", months)

			result.Points = max(result.Points-int(months), 0)
		}
	}

	return result
}

// GroupPunishments groups punishments by their category
// Todo: This probably has room for optimisation
func GroupPunishments(punishments *[]model.Punishment) map[string][]model.Punishment {
	result := make(map[string][]model.Punishment)

	for _, punishment := range *punishments {
		category, ok := result[punishment.Reason.Category]

		if ok {
			result[punishment.Reason.Category] = append(category, punishment)
		} else {
			result[punishment.Reason.Category] = []model.Punishment{punishment}
		}
	}

	return result
}
