package v1

import (
	"Observer/db"
	"Observer/db/options"
	"Observer/model"
	"errors"
	"github.com/gin-gonic/gin"
	"net/http"
	"strconv"
)

func getReasons(ctx *gin.Context) {
	grouped, err := strconv.ParseBool(ctx.DefaultQuery("grouped", "false"))

	if err != nil {
		ctx.Error(err)
		return
	}

	filter, err := strconv.ParseBool(ctx.DefaultQuery("filter", "false"))

	if err != nil {
		ctx.Error(err)
		return
	}

	reasons, err := db.Provider.GetReasons()
	var result []model.PunishmentReason

	if err != nil {
		ctx.Error(err)
		return
	}

	if filter {
		for _, reason := range *reasons {
			if !reason.InternalOnly {
				result = append(result, reason)
			}
		}
	} else {
		result = *reasons
	}

	if grouped {
		mapped := make(map[string][]model.PunishmentReason)

		for _, reason := range result {
			if category, ok := mapped[reason.Category]; ok {
				mapped[reason.Category] = append(category, reason)
			} else {
				mapped[reason.Category] = []model.PunishmentReason{reason}
			}
		}

		ctx.JSON(http.StatusOK, mapped)
	} else {
		ctx.JSON(http.StatusOK, result)
	}
}

func insertReason(ctx *gin.Context) {
	reason := &model.PunishmentReason{}
	err := ctx.BindJSON(reason)

	if err != nil {
		ctx.Error(err)
		return
	}

	err = db.Provider.UpsertReason(*reason, options.PatchReasonOptions{})

	if err != nil {
		ctx.Error(err)
		return
	}

	insertedReason, err := db.Provider.GetReason(reason.Name)

	if err != nil {
		ctx.Error(err)
		return
	}

	ctx.JSON(http.StatusOK, insertedReason)
}

func patchReason(ctx *gin.Context) {
	reason := &model.PunishmentReason{}

	err := ctx.BindJSON(reason)

	if err != nil {
		ctx.Error(err)
		return
	}

	err = db.Provider.UpsertReason(*reason, options.PatchReasonOptions{
		CascadePatchPunishments: true,
	})

	if err != nil {
		ctx.Error(err)
		return
	}

	updatedReason, err := db.Provider.GetReason(reason.Name)

	if err != nil {
		ctx.Error(err)
		return
	}

	ctx.JSON(http.StatusOK, updatedReason)

}

func deleteReason(ctx *gin.Context) {
	name := ctx.Param("name")

	if name == "" {
		ctx.Error(errors.New("no punishment reason provided"))
		return
	}

	deletePunishments, err := strconv.ParseBool(ctx.DefaultQuery("deletePunishments", "false"))
	if err != nil {
		ctx.Error(err)
		return
	}

	reason, err := db.Provider.GetReason(name)
	if err != nil {
		ctx.Error(err)
		return
	}

	err = db.Provider.DeleteReason(*reason, options.DeleteReasonOptions{
		CascadeDeletePunishments: deletePunishments,
	})

	if err != nil {
		ctx.Error(err)
		return
	}

	ctx.JSON(http.StatusOK, gin.H{"status": "deleted successfully"})
}
