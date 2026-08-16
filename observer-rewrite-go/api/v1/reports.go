package v1

import (
	"Observer/db"
	"Observer/model"
	"Observer/service"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"net/http"
)

func reportPlayer(ctx *gin.Context) {
	body := &model.PlayerReportData{}

	err := ctx.BindJSON(body)

	if err != nil {
		ctx.Error(err)
		return
	}

	logrus.Infof("Creating report: player=%v, reporter=%v,reason=%v,location=%v,replayId=%v", body.Player, body.Reporter, body.Reason, body.ServerLocation, body.ReplayID)

	report, err := service.CreateReport(body.Player, body.Reporter, body.Reason, body.ServerLocation, body.ReplayID)
	if err != nil {
		ctx.Error(err)
		return
	}

	ctx.JSON(http.StatusOK, report)
}

func getReportsBulk(ctx *gin.Context) {
	targetNames := []string{}

	err := ctx.BindJSON(&targetNames)

	if err != nil {
		ctx.Error(err)
		return
	}

	reports, err := db.Provider.GetReports(targetNames)

	if err != nil {
		ctx.Error(err)
		return
	}

	ctx.JSON(http.StatusOK, reports)
}

func getPlayerReport(ctx *gin.Context) {
	xuid := ctx.Param("xuid")

	report, err := db.Provider.GetReport(xuid)

	if err != nil {
		ctx.Error(err)
		return
	}

	if report == nil {
		ctx.JSON(http.StatusNotFound, gin.H{"error": "player was not reported"})
	} else {
		ctx.JSON(http.StatusOK, report)
	}

}

func deleteReports(ctx *gin.Context) {
	//xuid := ctx.Param("xuid")
	//resolution := ctx.Query("resolution")

}

func markTraineeClaimed(ctx *gin.Context) {

}

func getBestReports(ctx *gin.Context) {

}
