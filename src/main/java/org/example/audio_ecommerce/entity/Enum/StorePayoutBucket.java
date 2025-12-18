package org.example.audio_ecommerce.entity.Enum;

public enum StorePayoutBucket {
    PENDING,              // delivered + eligible=false + isPayout=false + not returned
    ELIGIBLE_NOT_PAYOUT,  // delivered + eligible=true  + isPayout=false + not returned (=> fee payable)
    PAYOUT_DONE           // delivered + eligible=true  + isPayout=true  + not returned (=> available)
}