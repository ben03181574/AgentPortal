MATCH (n) DETACH DELETE n;

// 退款流程（簡化版）
CREATE (s:SopTemplate {
  code: 'REFUND_FLOW',
  name: '退款流程',
  description: '處理客戶退款申請的標準作業流程',
  tags: ['退款', '客服']
});

CREATE (st1:SopStep {
  key: 'REFUND_START',
  name: '接收退款需求',
  description: '接收使用者的退款理由與訂單編號，確認該訂單符合退款條件。',
  stepType: 'USER_INPUT',
  order: 1
});

CREATE (st2:SopStep {
  key: 'CHECK_ELIGIBILITY',
  name: '檢查退款資格',
  description: '檢查訂單是否在未出貨/未使用且金額需要大於0且小於1000。',
  stepType: 'DECISION',
  order: 2
});

CREATE (st3:SopStep {
  key: 'DO_REFUND',
  name: '執行退款',
  description: '透過金流系統執行退款，並紀錄退款交易編號。',
  stepType: 'ACTION',
  order: 3
});

CREATE (st4:SopStep {
  key: 'REJECT_REFUND',
  name: '拒絕退款並說明原因',
  description: '告知使用者退款無法受理，提供原因與後續建議。',
  stepType: 'ACTION',
  order: 3
});

CREATE (st5:SopStep {
  key: 'NOTIFY_USER',
  name: '通知使用者結果',
  description: '將退款成功或拒絕的結果通知給使用者。',
  stepType: 'ACTION',
  order: 4
});

MATCH (s:SopTemplate {code: 'REFUND_FLOW'}), (st1:SopStep {key: 'REFUND_START'})
CREATE (s)-[:START_STEP]->(st1),
       (s)-[:HAS_STEP]->(st1);

MATCH (s:SopTemplate {code: 'REFUND_FLOW'}), (st2:SopStep {key: 'CHECK_ELIGIBILITY'}),
      (st3:SopStep {key: 'DO_REFUND'}), (st4:SopStep {key: 'REJECT_REFUND'}),
      (st5:SopStep {key: 'NOTIFY_USER'}), (st1:SopStep {key: 'REFUND_START'})
CREATE (s)-[:HAS_STEP]->(st2),
       (s)-[:HAS_STEP]->(st3),
       (s)-[:HAS_STEP]->(st4),
       (s)-[:HAS_STEP]->(st5),
       (st1)-[:NEXT {conditionType: 'ALWAYS', conditionText: ''}]->(st2),
       (st2)-[:NEXT {conditionType: 'IF', conditionText: '訂單符合退款條件'}]->(st3),
       (st2)-[:NEXT {conditionType: 'ELSE', conditionText: '訂單不符合退款條件'}]->(st4),
       (st3)-[:NEXT {conditionType: 'ALWAYS', conditionText: ''}]->(st5),
       (st4)-[:NEXT {conditionType: 'ALWAYS', conditionText: ''}]->(st5);

MATCH (n) RETURN n;