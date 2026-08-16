package mongo

import (
	options2 "Observer/db/options"
	"Observer/model"
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo/options"
)

func (prov *MongoDataProvider) GetReason(name string) (*model.PunishmentReason, error) {
	result := prov.Client.Database(databaseName).Collection("punishment_reasons").FindOne(context.TODO(), bson.M{"name": name})

	if result.Err() != nil {
		return nil, result.Err()
	}

	var reason model.PunishmentReason
	err := result.Decode(&reason)

	if err != nil {
		return nil, err
	}

	return &reason, nil
}

func (prov *MongoDataProvider) UpsertReason(reason model.PunishmentReason, opts options2.PatchReasonOptions) error {
	mongoOpts := options.Replace().SetUpsert(true)
	_, err := prov.Client.Database(databaseName).Collection("punishment_reasons").ReplaceOne(context.TODO(), bson.M{"name": reason.Name}, reason, mongoOpts)

	if err == nil && opts.CascadePatchPunishments {
		_, err := prov.Client.Database(databaseName).Collection("punishments").UpdateMany(context.TODO(), bson.M{"reason.name": reason.Name}, bson.D{{"$set", bson.D{{"reason", reason}}}})

		return err
	}

	return err
}

func (prov *MongoDataProvider) DeleteReason(reason model.PunishmentReason, opts options2.DeleteReasonOptions) error {
	_, err := prov.Client.Database(databaseName).Collection("punishment_reasons").DeleteOne(context.TODO(), bson.M{"name": reason.Name})

	if err == nil && opts.CascadeDeletePunishments {
		return prov.DeletePunishmentsByReason(reason)
	}

	return err
}

func (prov *MongoDataProvider) GetReasons() (*[]model.PunishmentReason, error) {
	cursor, err := prov.Client.Database(databaseName).Collection("punishment_reasons").Find(context.TODO(), bson.M{})

	if err != nil {
		return nil, err
	}

	defer cursor.Close(context.TODO())

	var results []model.PunishmentReason

	err = cursor.All(context.TODO(), &results)

	if err != nil {
		return nil, err
	}

	return &results, nil
}
