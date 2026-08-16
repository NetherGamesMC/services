package model

type EvidenceType string

const (
	EvidenceTypeLink   EvidenceType = "LINK"
	EvidenceTypeText   EvidenceType = "TEXT"
	EvidenceTypeReplay EvidenceType = "REPLAY_ID"
	EvidenceTypeAWS    EvidenceType = "AWS_MANAGED"
)

type PunishmentEvidence struct {
	PunishmentId string       `json:"punishmentId"`
	EvidenceID   uint64       `json:"evidenceId"`
	Player       string       `json:"player"`
	AttachedBy   string       `json:"attachedBy"`
	Type         EvidenceType `json:"type"`
	Data         string       `json:"data"`
	Note         string       `json:"note"`
}

type TemporaryEvidence struct {
	Accepted     bool   `json:"accepted"`
	PunishmentID string `json:"punishmentId"`
	IssuedID     string `json:"issuedId"`
}
