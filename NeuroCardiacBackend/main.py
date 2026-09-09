from fastapi import FastAPI, UploadFile, File, HTTPException
from pydantic import BaseModel, Field
import uvicorn
import tensorflow as tf
from PIL import Image
import numpy as np
import io
import joblib
import pandas as pd
import asyncio
import os
from dotenv import load_dotenv

# Load configuration from .env (see .env.example)
load_dotenv()

app = FastAPI(title="Brain Tumor & Heart Disease AI API")

# Deep Learning Model (Brain Tumor)
BRAIN_MODEL_PATH = os.getenv("BRAIN_MODEL_PATH", "models/deep_learning_model.h5")
try:
    model = tf.keras.models.load_model(BRAIN_MODEL_PATH)
    print("Brain Tumor Model loaded successfully")
except Exception as e:
    print(f"Error loading model: {e}")

# Machine Learning Model (Heart Disease)
HEART_MODEL_PATH = os.getenv("HEART_MODEL_PATH", "models/model_learning_model.pkl")
try:
    heart_data = joblib.load(HEART_MODEL_PATH)
    heart_model = heart_data['model']
    heart_scaler = heart_data['scaler']
    print("Heart Disease Model file loaded successfully")
except Exception as e:
    print(f"Error loading Heart Disease model: {e}")
TARGET_IMAGE_SIZE = (299, 299)
CLASS_NAMES = ["Glioma", "Meningioma", "No Tumor", "Pituitary"]
class HeartClinicalData(BaseModel):
    Age: int = Field(..., ge=1, le=120, description="Age in years")
    Sex: str
    ChestPain: str
    RestingBP: int = Field(..., ge=0, le=300, description="Resting blood pressure in mm Hg")
    Cholesterol: int = Field(..., ge=0, le=600, description="Serum cholesterol in mg/dl")
    FastingBS: str
    MaxHR: int = Field(..., ge=60, le=220, description="Maximum heart rate achieved")
    ExAngina: str
    Oldpeak: float = Field(..., ge=-5.0, le=10.0, description="ST depression induced by exercise relative to rest")
    ST_Slope: str

@app.post("/predict/image")
async def predict_image(file: UploadFile = File(...)):
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image.")
    try:
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")
        image = image.resize(TARGET_IMAGE_SIZE)
        image_array = np.array(image)
        image_array = image_array / 255.0
        image_array = np.expand_dims(image_array, axis=0)
        predictions = await asyncio.to_thread(model.predict, image_array)
        predicted_class_index = np.argmax(predictions[0])
        predicted_class_name = CLASS_NAMES[predicted_class_index]
        confidence = float(predictions[0][predicted_class_index])
        return {
            "prediction": predicted_class_name,
            "confidence": round(confidence * 100, 2),
            "filename": file.filename
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/predict/heart")
async def predict_heart(data: HeartClinicalData):
    try:
        sex_val = 1 if data.Sex.upper() in ["M", "MALE", "1"] else 0
        fasting_bs_val = 1 if data.FastingBS.upper() in ["YES", "Y", "1"] else 0
        ex_angina_val = 1 if data.ExAngina.upper() in ["YES", "Y", "1"] else 0
        cp_map = {"ASY": 0, "ATA": 1, "NAP": 2, "TA": 3}
        cp_val = cp_map.get(data.ChestPain.upper(), 0)
        st_map = {"DOWN": 0, "FLAT": 1, "UP": 2}
        st_val = st_map.get(data.ST_Slope.upper(), 1)
        df = pd.DataFrame([{
            "Age": data.Age,
            "ChestPainType": cp_val,
            "RestingBP": data.RestingBP,
            "Cholesterol": data.Cholesterol,
            "FastingBS": fasting_bs_val,
            "MaxHR": data.MaxHR,
            "Oldpeak": data.Oldpeak,
            "ST_Slope": st_val,
            "Sex_M": sex_val,
            "ExerciseAngina_Y": ex_angina_val
        }])
        scaled_features = heart_scaler.transform(df)
        prediction = await asyncio.to_thread(heart_model.predict, scaled_features)
        diagnosis = "High Risk of Heart Disease" if prediction[0] == 1 else "Low Risk"

        return {
            "prediction": diagnosis,
            "confidence": 0.0,
            "filename": "Clinical Assessment"
        }

    except Exception as e:
        print("\n" + "=" * 50)
        print(f"CRASH DETAILS: {repr(e)}")
        import traceback
        traceback.print_exc()
        print("=" * 50 + "\n")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "8000"))
    uvicorn.run(app, host=host, port=port)
