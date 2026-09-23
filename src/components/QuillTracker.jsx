import React from 'react';

export const STAGES = ['CONFIRMED', 'PROCESSING', 'PACKED', 'SHIPPED', 'IN_TRANSIT', 'CUSTOMS', 'DELIVERED'];
export const STAGE_LABEL = {
  PENDING_PAYMENT: 'Awaiting payment', CONFIRMED: 'Confirmed', PROCESSING: 'Processing',
  PACKED: 'Packed', SHIPPED: 'Shipped', IN_TRANSIT: 'In transit', CUSTOMS: 'Customs',
  DELIVERED: 'Delivered', CANCELLED: 'Cancelled',
};

export default function QuillTracker({ status }) {
  const stageIdx = STAGES.indexOf(status);
  if (status === 'CANCELLED') {
    return <div className="role-note" style={{ marginTop: 16 }}>✕ This order was cancelled.</div>;
  }
  return (
    <div className="quill-tracker">
      {STAGES.map((s, i) => (
        <div key={s} className={`quill-seg ${i < stageIdx ? 'done' : ''} ${i === stageIdx ? 'current' : ''}`}>
          <div className="coil">{i <= stageIdx ? '✓' : ''}</div>
          <div className="lbl">{STAGE_LABEL[s]}</div>
        </div>
      ))}
    </div>
  );
}
