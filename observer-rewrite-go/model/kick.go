package model

type Kick struct {
	XboxID   string `json:"xuid"`
	Reason   string `json:"reason"`
	IssuedBy string `json:"issuedBy"`
}
