# Digital Forensics Detector Model

Required model filename:

`efficientdet_lite1.tflite`

Expected path in app assets:

`app/src/main/assets/models/efficientdet_lite1.tflite`

## Recommended source

Use TensorFlow Lite EfficientDet-Lite1 (metadata) from TF Hub.

TF Hub page:
- https://whatsapp.com/channel/0029VbCNNEzLCoX9wla4yb2e/1252

Direct TFLite download:
- https://whatsapp.com/channel/0029VbCNNEzLCoX9wla4yb2e/1252

## Notes

- Keep filename exactly `efficientdet_lite1.tflite`.
- This is the only supported runtime detector in the current architecture.
- COCO class mapping is used for event typing (`PERSON`, `VEHICLE`, `PET`, `OBJECT`).
