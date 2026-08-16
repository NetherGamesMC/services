package v1

import (
	"Observer/db"
	"Observer/db/options"
	"Observer/model"
	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
	"net/http"
	"strings"
)

var stringToType = map[string]model.TracingType{
	"IP":             model.TracingTypeIP,
	"SELF_SIGNED_ID": model.TracingTypeSelfSignedID,
	"DEVICE_ID":      model.TracingTypeDeviceID,
}

type tracingResult struct {
	Depth   int                  `json:"depth"`
	Matches map[string]*[]string `json:"tracingMatches"`
}

func tracePlayer(ctx *gin.Context) {
	xuid := ctx.Param("xuid")
	searchConditions := ctx.Query("searchConditions")

	var conditions []model.TracingType

	if searchConditions == "" {
		conditions = options.AllTraceSearchConditions
	} else {
		for _, item := range strings.Split(searchConditions, ",") {
			if translated, ok := stringToType[item]; ok {
				conditions = append(conditions, translated)
			} else {
				// todo this should later throw an error
			}
		}
	}

	result, err := db.Provider.DoTrace(xuid, options.DoTraceOptions{
		SearchConditions: conditions,
	})

	if err != nil {
		logrus.Errorf("Error while running trace query: %v", err.Error())
		ctx.Error(err)
		return
	}

	response := tracingResult{Depth: 0, Matches: make(map[string]*[]string)}

	response.Matches["IP"] = result.Matches[model.TracingTypeIP]
	response.Matches["DEVICE_ID"] = result.Matches[model.TracingTypeDeviceID]
	response.Matches["SELF_SIGNED_ID"] = result.Matches[model.TracingTypeSelfSignedID]

	ctx.JSON(http.StatusOK, response)

}

func unlinkPlayer(ctx *gin.Context) {

}
