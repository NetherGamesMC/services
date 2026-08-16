package model

import "time"

type PunishmentType string
type GroupedPunishments map[string][]Punishment

const (
	PunishmentTypeBan  PunishmentType = "BAN"
	PunishmentTypeMute PunishmentType = "MUTE"
)

type Punishment struct {
	ID         string                `json:"id" bson:"_id"`
	XboxID     string                `json:"xuid"`
	IssuedBy   string                `json:"issuedBy"`
	Note       string                `json:"note"`
	Reason     PunishmentReason      `json:"reason"`
	Permanent  bool                  `json:"permanent"`
	IssuedAt   int64                 `json:"issuedAt"`
	ValidUntil int64                 `json:"validUntil"`
	Evidence   *[]PunishmentEvidence `json:"evidence,omitempty" bson:"-"`
}

func (p Punishment) IsActive() bool {
	return p.Permanent || (p.ValidUntil > time.Now().Unix())
}

type PunishmentReason struct {
	Name               string         `json:"name"`
	Type               PunishmentType `json:"type"`
	HasRollback        bool           `json:"hasRollback"`
	AdminOnly          bool           `json:"adminOnly"`
	InternalOnly       bool           `json:"internalOnly"`
	NonAppealable      bool           `json:"nonAppealable"`
	FlagRecentEvidence bool           `json:"flagRecentEvidence"`
	Points             int            `json:"points"`
	Category           string         `json:"category"`
}
