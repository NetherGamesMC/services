package request

import "Observer/model"

type PlayerStatus struct {
	Ban    *model.Punishment              `json:"ban"`
	Mute   *model.Punishment              `json:"mute"`
	Points *map[string]model.PointMapping `json:"points"`
}
