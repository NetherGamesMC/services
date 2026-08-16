package options

import (
	"Observer/model"
)

var AllFilterTypes = []model.PunishmentType{model.PunishmentTypeBan, model.PunishmentTypeMute}
var AllTraceSearchConditions = []model.TracingType{model.TracingTypeIP, model.TracingTypeDeviceID, model.TracingTypeSelfSignedID}

type GetPunishmentOptions struct {
	ActiveOnly         bool
	ResolveEvidence    bool
	IncludeAltAccounts bool
	FilterTypes        []model.PunishmentType
}

func (opts GetPunishmentOptions) GetFilterTypes() []model.PunishmentType {
	if len(opts.FilterTypes) == 0 {
		return AllFilterTypes
	}

	return opts.FilterTypes
}

type DoTraceOptions struct {
	SearchConditions []model.TracingType
}

func (opts DoTraceOptions) GetSearchConditions() []model.TracingType {
	if opts.SearchConditions == nil || len(opts.SearchConditions) == 0 {
		return AllTraceSearchConditions
	}

	return opts.SearchConditions
}

type DeleteReasonOptions struct {
	CascadeDeletePunishments bool
}

type PatchReasonOptions struct {
	CascadePatchPunishments bool
}
