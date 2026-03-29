MATCH (n) DETACH DELETE n;

CREATE (s:SopTemplate {
  code: 'SIMPLE_REFUND_FLOW',
  name: '簡易退款流程',
  description: '收集訂單編號與退款原因後執行退款'
});

CREATE (st1:SopStep {
  key: 'ASK_ORDER_ID',
  name: '取得訂單編號',
  description: '請提供您的訂單編號。',
  stepType: 'USER_INPUT'
});

CREATE (st2:SopStep {
  key: 'ASK_REASON',
  name: '取得退款原因',
  description: '請提供您的退款原因。',
  stepType: 'USER_INPUT'
});

CREATE (st3:SopStep {
  key: 'DO_REFUND',
  name: '執行退款',
  description: '系統執行退款流程。',
  stepType: 'ACTION'
});

CREATE (st4:SopStep {
  key: 'END',
  name: '流程結束',
  description: 'SOP END',
  stepType: 'END'
});

MATCH (s:SopTemplate {code: 'SIMPLE_REFUND_FLOW'}),
      (st1:SopStep {key: 'ASK_ORDER_ID'}),
      (st2:SopStep {key: 'ASK_REASON'}),
      (st3:SopStep {key: 'DO_REFUND'}),
      (st4:SopStep {key: 'END'})

CREATE
  (s)-[:START_STEP]->(st1),

  (s)-[:HAS_STEP]->(st1),
  (s)-[:HAS_STEP]->(st2),
  (s)-[:HAS_STEP]->(st3),
  (s)-[:HAS_STEP]->(st4),

  (st1)-[:NEXT {conditionType: 'ALWAYS'}]->(st2),
  (st2)-[:NEXT {conditionType: 'ALWAYS'}]->(st3),
  (st3)-[:NEXT {conditionType: 'ALWAYS'}]->(st4);

MATCH (n) RETURN n;