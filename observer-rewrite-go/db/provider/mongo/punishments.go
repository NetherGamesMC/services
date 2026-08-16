package mongo

import (
	"Observer/db/options"
	"Observer/model"
	"context"
	"github.com/sirupsen/logrus"
	"go.mongodb.org/mongo-driver/bson"
	options2 "go.mongodb.org/mongo-driver/mongo/options"
	"time"
)

func (prov *MongoDataProvider) GetPunishments(XboxID string, opts options.GetPunishmentOptions) (*[]model.Punishment, error) {
	var targets []string

	if opts.IncludeAltAccounts {
		traceResult, err := prov.DoTrace(XboxID, options.DoTraceOptions{
			SearchConditions: []model.TracingType{model.TracingTypeDeviceID, model.TracingTypeSelfSignedID},
		})

		if err != nil {
			return nil, err
		}

		for resultType, results := range traceResult.Matches {
			logrus.Infof("Tracing Type %v for player %v showed results: %v", resultType, XboxID, results)
		}

		targets = append(traceResult.ToStrings(), XboxID)
	} else {
		targets = []string{XboxID}
	}

	filter := bson.M{
		"xuid": bson.M{"$in": targets},
		"reason.type": bson.M{
			"$in": opts.GetFilterTypes(),
		},
	}

	if opts.ActiveOnly {
		filter = and(filter, bson.M{"$or": []bson.M{
			{
				"validUntil": bson.M{"$gt": time.Now().Unix()},
			},
			{
				"permanent": bson.M{"$eq": true},
			},
		}})
	}

	result := []model.Punishment{}

	cursor, err := prov.Client.Database(databaseName).Collection("punishments").Find(context.TODO(), filter)

	if err != nil {
		return nil, err
	}

	defer cursor.Close(context.Background())

	err = cursor.All(context.TODO(), &result)

	if err != nil {
		return nil, err
	}

	return &result, nil
}

func (prov *MongoDataProvider) UpsertPunishment(punishment model.Punishment) error {
	opts := options2.Replace().SetUpsert(true)
	_, err := prov.Client.Database(databaseName).Collection("punishments").ReplaceOne(context.Background(), bson.M{"_id": punishment.ID}, punishment, opts)

	return err
}

func (prov *MongoDataProvider) DeletePunishment(punishment model.Punishment) error {
	_, err := prov.Client.Database(databaseName).Collection("punishments").DeleteOne(context.Background(), bson.M{"_id": punishment.ID})

	return err
}

func (prov *MongoDataProvider) DeletePunishmentsByReason(reason model.PunishmentReason) error {
	_, err := prov.Client.Database(databaseName).Collection("punishments").DeleteMany(context.Background(), bson.M{"reason.name": reason.Name})

	return err
}

func (prov *MongoDataProvider) GetPunishmentById(id string) (*model.Punishment, error) {
	result, err := prov.Client.Database(databaseName).Collection("punishments").Find(context.TODO(), bson.M{"_id": id})

	if err != nil {
		return nil, err
	}

	if result.Next(context.TODO()) {
		punishment := &model.Punishment{}
		err := result.Decode(punishment)
		if err != nil {
			return nil, err
		}

		return punishment, nil
	} else {
		return nil, nil
	}
}

func (prov *MongoDataProvider) GetPunishmentsMultiple(XboxIDs []string, opts options.GetPunishmentOptions) (*[]model.Punishment, error) {
	var targets []string

	logrus.Infof("Initial target list: %v", XboxIDs)

	if opts.IncludeAltAccounts {
		for _, xboxId := range XboxIDs {
			traceResult, err := prov.DoTrace(xboxId, options.DoTraceOptions{})

			if err != nil {
				return nil, err
			}

			for tracingType, result := range traceResult.Matches {
				logrus.Infof("results for tracing type %v for player %v are: %v", tracingType, xboxId, *result)
			}

			logrus.Infof("Trace Results for %v: %v", xboxId, traceResult.ToStrings())

			targets = append(traceResult.ToStrings(), xboxId)
		}
	} else {
		targets = XboxIDs
	}

	filter := bson.M{
		"xuid": bson.M{"$in": targets},
		"reason.type": bson.M{
			"$in": opts.GetFilterTypes(),
		},
	}

	if opts.ActiveOnly {
		filter = and(filter, bson.M{"$or": []bson.M{
			{
				"validUntil": bson.M{"$gt": time.Now().Unix()},
			},
			{
				"permanent": bson.M{"$eq": true},
			},
		}})
	}

	var result []model.Punishment

	cursor, err := prov.Client.Database(databaseName).Collection("punishments").Find(context.TODO(), filter)

	if err != nil {
		return nil, err
	}

	defer cursor.Close(context.Background())

	err = cursor.All(context.TODO(), &result)

	if err != nil {
		return nil, err
	}

	return &result, nil
}
