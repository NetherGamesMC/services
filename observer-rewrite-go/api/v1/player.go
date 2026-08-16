package v1

import (
	"Observer/db"
	"Observer/db/options"
	"Observer/model"
	"Observer/model/request"
	"Observer/service"
	"github.com/gin-gonic/gin"
	"net/http"
	"strconv"
)

func getPlayerStatus(ctx *gin.Context) {
	xboxId := ctx.Param("xuid")

	includePointMaps, err := strconv.ParseBool(ctx.DefaultQuery("includePointMaps", "false"))
	if err != nil {
		ctx.Error(err)
		return
	}

	status := &request.PlayerStatus{}
	activePunishments, err := db.Provider.GetPunishments(xboxId, options.GetPunishmentOptions{
		ActiveOnly:         true,
		IncludeAltAccounts: true,
		ResolveEvidence:    true,
	})

	if err != nil {
		ctx.Error(err)
		return
	}

	for _, punishment := range *activePunishments {
		switch punishment.Reason.Type {
		case model.PunishmentTypeBan:
			status.Ban = &punishment
			break
		case model.PunishmentTypeMute:
			status.Mute = &punishment
		}
	}

	if includePointMaps {
		punishments, err := db.Provider.GetPunishments(xboxId, options.GetPunishmentOptions{
			IncludeAltAccounts: true,
		})

		if err != nil {
			ctx.Error(err)
			return
		}

		categories := service.GroupPunishments(punishments)
		result := map[string]model.PointMapping{}
		for category, punishments := range categories {
			result[category] = service.CalculatePunishmentPoints(punishments)
		}

		status.Points = &result
	}

	ctx.JSON(http.StatusOK, status)
}

func postPointMapping(ctx *gin.Context) {

}

func getXUIDAddresses(ctx *gin.Context) {

}

func getPointExplanation(ctx *gin.Context) {

}
