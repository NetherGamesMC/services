package model

type TracingType int

const (
	TracingTypeIP           TracingType = 0
	TracingTypeDeviceID     TracingType = 1
	TracingTypeSelfSignedID TracingType = 2
)

type AltTracingDataset struct {
	XboxID        string   `json:"xuid"`
	IPs           []string `json:"ip"`
	DeviceIDs     []string `json:"deviceId"`
	SelfSignedIDs []string `json:"selfSignedId"`
}

type AltTracingPushData struct {
	Username       string `json:"username"`
	XboxID         string `json:"xuid"`
	IP             string `json:"ip"`
	DeviceID       string `json:"deviceId"`
	SelfSignedID   string `json:"selfSignedId"`
	UUID           string `json:"uuid"`
	ClientRandomID string `json:"clientRandomId"`
}

type TracingSearchEntry struct {
	Depth            int           `json:"depth"`
	XboxIDList       []string      `json:"xuidList"`
	Exclusions       []string      `json:"exclusions"`
	SearchConditions []TracingType `json:"searchConditions"`
}

type TracingSearchResult struct {
	Depth   int                       `json:"depth"`
	Matches map[TracingType]*[]string `json:"tracingMatches"`
}

type TracingWhitelistEntry struct {
	Origin    string `json:"originXuid"`
	Exclusion string `json:"exclusionXuid"`
}

// ToStrings just flattens this object into an array of strings with the XboxIDs of all traced players
// todo: this is probably a tradeoff between CPU time and memory complexity. Maybe optimize? Elements must be unique!
func (result *TracingSearchResult) ToStrings() []string {
	temp := make(map[string]bool)

	for _, match := range *result.Matches[TracingTypeIP] {
		temp[match] = true
	}

	for _, match := range *result.Matches[TracingTypeDeviceID] {
		temp[match] = true
	}

	for _, match := range *result.Matches[TracingTypeSelfSignedID] {
		temp[match] = true
	}

	res := make([]string, 0, len(temp))
	for xboxId, valid := range temp {
		if valid {
			res = append(res, xboxId)
		}
	}

	return res
}
