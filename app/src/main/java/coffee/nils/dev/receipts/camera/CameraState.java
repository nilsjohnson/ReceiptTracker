package coffee.nils.dev.receipts.camera;

public enum CameraState
{
    /**
     * Showing camera preview.
     */
    STATE_PREVIEW,

    /**
     * Waiting for the focus to be locked.
     */
    STATE_WAITING_LOCK,

    /**
     * Waiting for the exposure to be precapture state.
     */
   STATE_WAITING_PRECAPTURE,

    /**
     * Waiting for the exposure state to be something other than precapture.
     */
  STATE_WAITING_NON_PRECAPTURE,

    /**
     * Picture was taken.
     */
    STATE_PICTURE_TAKEN,

    /**
     * User choosing to keep or discard image.
     */
    STATE_IMAGE_REVIEW
}