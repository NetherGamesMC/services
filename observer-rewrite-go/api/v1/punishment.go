package v1

import (
	"Observer/db"
	"Observer/db/options"
	"Observer/model"
	"Observer/service"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"net/http"
	"strconv"
	"sync"
)

type punishmentCreationBody struct {
	XboxID string `json:"xuid"`
	Name   string `json:"name"`
	Issuer string `json:"issuer"`
	Reason string `json:"reason"`
	Note   string `json:"note"`
}

func createPunishment(ctx *gin.Context) {
	creationData := punishmentCreationBody{}

	err := ctx.BindJSON(&creationData)

	if err != nil {
		ctx.Error(err)
		return
	}

	reason, err := db.Provider.GetReason(creationData.Reason)

	if err != nil {
		ctx.Error(err)
		return
	}

	if reason == nil {
		// todo proper error structures
		ctx.JSON(http.StatusNotFound, gin.H{
			"error": "punishment reason not found",
		})

		return
	}

	punishment, err := service.CreatePlayerPunishment(creationData.XboxID, *reason, creationData.Issuer, creationData.Note)

	if err != nil {
		ctx.Error(err)
		return
	}

	ctx.JSON(http.StatusOK, punishment)
}

type punishmentRequestData struct {
	XboxID          string                 `json:"xuid"`
	ActiveOnly      bool                   `json:"activeOnly"`
	PunishmentTypes []model.PunishmentType `json:"punishmentTypes"`
}

type bulkPunishment struct {
	XboxID      string             `json:"xuid"`
	Punishments []model.Punishment `json:"punishments"`
}

type bulkPunishmentsGrouped struct {
	XboxID      string                        `json:"xuid"`
	Punishments map[string][]model.Punishment `json:"punishments"`
}

func getPunishments(ctx *gin.Context) {
	data := &[]punishmentRequestData{}

	err := ctx.BindJSON(data)

	if err != nil {
		ctx.Error(err)
		return
	}

	grouped, err := strconv.ParseBool(ctx.DefaultQuery("grouped", "false"))

	if err != nil {
		ctx.Error(err)
		return
	}

	playerGroupedPunishments := make(map[string][]model.Punishment)

	wg := &sync.WaitGroup{}
	for _, request := range *data {
		wg.Add(1)
		go func() {
			defer wg.Done()

			playerResult, err := db.Provider.GetPunishments(request.XboxID, options.GetPunishmentOptions{
				ActiveOnly:         request.ActiveOnly,
				IncludeAltAccounts: true,
				ResolveEvidence:    true,
				FilterTypes:        request.PunishmentTypes,
			})

			if err != nil {
				logrus.Errorf("Error while trying to resolve punishments for %v: %v", request.XboxID, err)
				ctx.Error(err)
			} else {
				playerGroupedPunishments[request.XboxID] = *playerResult
			}
		}()
	}

	wg.Wait()

	if grouped {
		result := []bulkPunishmentsGrouped{}

		for player, groupedPunishments := range playerGroupedPunishments {
			categoryGrouped := make(map[string][]model.Punishment)

			for _, punishment := range groupedPunishments {
				if category, ok := categoryGrouped[punishment.Reason.Category]; ok {
					categoryGrouped[punishment.Reason.Category] = append(category, punishment)
				} else {
					categoryGrouped[punishment.Reason.Category] = []model.Punishment{punishment}
				}
			}

			result = append(result, bulkPunishmentsGrouped{XboxID: player, Punishments: categoryGrouped})
		}

		ctx.JSON(http.StatusOK, result)
	} else {
		result := []bulkPunishment{}
		for player, playerPunishments := range playerGroupedPunishments {
			result = append(result, bulkPunishment{XboxID: player, Punishments: playerPunishments})
		}

		ctx.JSON(http.StatusOK, result)
	}

}

func getPunishmentsFor(ctx *gin.Context) {
	xuid := ctx.Param("xuid")
	// todo validate that this param actually exists. Really need to start developing an error handling pattern

	grouped, err := strconv.ParseBool(ctx.DefaultQuery("grouped", "false"))
	if err != nil {
		ctx.Error(err)
		return
	}

	activeOnly, err := strconv.ParseBool(ctx.DefaultQuery("activeOnly", "false"))
	if err != nil {
		ctx.Error(err)
		return
	}

	_, withAltAccounts := ctx.GetQuery("depth")

	punishments, err := db.Provider.GetPunishments(xuid, options.GetPunishmentOptions{
		ActiveOnly:         activeOnly,
		IncludeAltAccounts: withAltAccounts,
		ResolveEvidence:    true,
	})

	if err != nil {
		ctx.Error(err)
		return
	}

	if !grouped {
		ctx.JSON(http.StatusOK, []bulkPunishment{{XboxID: xuid, Punishments: *punishments}})
	} else {
		groupedPunishments := map[string][]model.Punishment{}

		for _, punishment := range *punishments {
			if category, ok := groupedPunishments[punishment.Reason.Category]; ok {
				groupedPunishments[punishment.Reason.Category] = append(category, punishment)
			} else {
				groupedPunishments[punishment.Reason.Category] = []model.Punishment{punishment}
			}
		}

		ctx.JSON(http.StatusOK, bulkPunishmentsGrouped{XboxID: xuid, Punishments: groupedPunishments})
	}
}

func getPunishmentById(ctx *gin.Context) {
	id := ctx.Param("id")

	punishment, err := db.Provider.GetPunishmentById(id)

	if err != nil {
		ctx.Error(err)
		return
	}

	if punishment == nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "punishment not found"})
	} else {
		ctx.JSON(http.StatusOK, punishment)
	}
}

func setPunishmentById(ctx *gin.Context) {

}

func deletePunishmentbyId(ctx *gin.Context) {

}

func searchForPunishment(ctx *gin.Context) {

}
