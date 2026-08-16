package mongo

import (
	"Observer/db/options"
	"Observer/model"
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"slices"
)

type TracingResult struct {
	IP           []TracedAltAccount `bson:"ip"`
	DeviceID     []TracedAltAccount `bson:"deviceId"`
	SelfSignedID []TracedAltAccount `bson:"selfSignedId"`
}

type TracedAltAccount struct {
	XboxID  string   `json:"xuid" bson:"_id"`
	Matches []string `json:"matches"`
}

func (prov *MongoDataProvider) DoTrace(XboxID string, options options.DoTraceOptions) (*model.TracingSearchResult, error) {
	isEnabled := func(tracingType model.TracingType) bool {
		return slices.Contains(options.GetSearchConditions(), tracingType)
	}

	pipeline := bson.A{
		bson.D{{"$match", bson.D{{"xuid", XboxID}}}},
		bson.D{
			{"$facet",
				bson.D{
					{"ip",
						ifElem(isEnabled(model.TracingTypeIP), bson.A{
							bson.D{
								{"$graphLookup",
									bson.D{
										{"from", "alt_accounts"},
										{"startWith", "$ip"},
										{"connectFromField", "ip"},
										{"connectToField", "ip"},
										{"as", "matches"},
										{"maxDepth", 0},
									},
								},
							},
							bson.D{
								{"$project",
									bson.D{
										{"_id", primitive.Null{}},
										{"matches", "$matches"},
									},
								},
							},
							bson.D{{"$unwind", "$matches"}},
							bson.D{{"$unwind", "$matches.ip"}},
							bson.D{
								{"$group",
									bson.D{
										{"_id", "$matches.xuid"},
										{"matches", bson.D{{"$addToSet", "$matches.ip"}}},
									},
								},
							},
						}),
					},
					{"selfSignedId",
						ifElem(isEnabled(model.TracingTypeSelfSignedID), bson.A{
							bson.D{
								{"$graphLookup",
									bson.D{
										{"from", "alt_accounts"},
										{"startWith", "$selfSignedId"},
										{"connectFromField", "selfSignedId"},
										{"connectToField", "selfSignedId"},
										{"as", "matches"},
										{"maxDepth", 0},
									},
								},
							},
							bson.D{
								{"$project",
									bson.D{
										{"_id", primitive.Null{}},
										{"matches", "$matches"},
									},
								},
							},
							bson.D{{"$unwind", "$matches"}},
							bson.D{{"$unwind", "$matches.selfSignedId"}},
							bson.D{
								{"$group",
									bson.D{
										{"_id", "$matches.xuid"},
										{"matches", bson.D{{"$addToSet", "$matches.selfSignedId"}}},
									},
								},
							},
						}),
					},
					{"deviceId",
						ifElem(isEnabled(model.TracingTypeDeviceID), bson.A{
							bson.D{
								{"$graphLookup",
									bson.D{
										{"from", "alt_accounts"},
										{"startWith", "$deviceId"},
										{"connectFromField", "deviceId"},
										{"connectToField", "deviceId"},
										{"as", "matches"},
										{"maxDepth", 0},
									},
								},
							},
							bson.D{
								{"$project",
									bson.D{
										{"_id", primitive.Null{}},
										{"matches", "$matches"},
									},
								},
							},
							bson.D{{"$unwind", "$matches"}},
							bson.D{{"$unwind", "$matches.deviceId"}},
							bson.D{
								{"$group",
									bson.D{
										{"_id", "$matches.xuid"},
										{"matches", bson.D{{"$addToSet", "$matches.deviceId"}}},
									},
								},
							},
						}),
					},
				},
			},
		},
	}

	cursor, err := prov.Tracing.Aggregate(context.TODO(), pipeline)

	if err != nil {
		println("query error")
		return nil, err
	}

	defer cursor.Close(context.TODO())

	queryResults := []TracingResult{}

	err = cursor.All(context.TODO(), &queryResults)
	if err != nil {
		println("cursor error")
		return nil, err
	}
	queryResult := queryResults[0]

	result := &model.TracingSearchResult{}
	result.Matches = make(map[model.TracingType]*[]string)

	var ipResults []string

	for _, account := range queryResult.IP {
		ipResults = append(ipResults, account.XboxID)
	}

	var deviceIdResults []string
	for _, account := range queryResult.DeviceID {
		deviceIdResults = append(deviceIdResults, account.XboxID)
	}

	var selfSignedIdResults []string
	for _, account := range queryResult.SelfSignedID {
		selfSignedIdResults = append(selfSignedIdResults, account.XboxID)
	}

	result.Matches[model.TracingTypeIP] = &ipResults
	result.Matches[model.TracingTypeDeviceID] = &deviceIdResults
	result.Matches[model.TracingTypeSelfSignedID] = &selfSignedIdResults

	return result, nil
}
