package com.jiefzz.extension.infrastructure.impl;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.jiefzz.ejoker.domain.AbstractAggregateRoot;
import com.jiefzz.ejoker.domain.IllegalAggregateRootIdException;
import com.jiefzz.ejoker.utils.RelationshipTreeUtil;
import com.jiefzz.ejoker.utils.RelationshipTreeUtilCallbackInterface;
import com.jiefzz.extension.infrastructure.IRepository;
import com.jiefzz.extension.infrastructure.impl.utils.MongoObjectRevertUtil;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


@Service
public class RepositoryOnMongoImpl implements IRepository {

	@Resource
	private DB mongoClientDB;

	private final String collectionName = "Version";
	DBCollection col;
	private RelationshipTreeUtil<DBObject, BasicDBList> relationshipTreeUtil = new RelationshipTreeUtil<DBObject, BasicDBList>(new BuilderToolSet());


	@PostConstruct
	private void init(){
		col = mongoClientDB.getCollection(collectionName);
	}

	@Override
	public void sotrage(AbstractAggregateRoot<?> ar) throws Exception {
		if ( ar.getId()==null )
			throw new IllegalAggregateRootIdException("AggregateRoot has wrong id!");
		col.save(relationshipTreeUtil.getTreeStructureMap(ar));
	}

	@Override
	public <TAggregateRootId, TAggregateRoot extends AbstractAggregateRoot<TAggregateRootId>>
	AbstractAggregateRoot<TAggregateRootId> get(TAggregateRootId id, TAggregateRoot obj)
			throws Exception {
		return MongoObjectRevertUtil.dbObject2Bean(col.findOne(id.toString()), obj);
	}

	@Override
	public <TAggregateRootId, TAggregateRoot extends AbstractAggregateRoot<TAggregateRootId>>
	AbstractAggregateRoot<TAggregateRootId> get(TAggregateRootId id, Class<TAggregateRoot> clazz)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * ongo直接存储对象的树形结构构造方法
	 * @author JiefzzLon
	 *
	 */
	public class BuilderToolSet implements RelationshipTreeUtilCallbackInterface<DBObject, BasicDBList> {

		@Override
		public DBObject createNode() throws Exception {
			return new BasicDBObject();
		}

		@Override
		public BasicDBList createValueSet() throws Exception {
			return new BasicDBList();
		}

		@Override
		public boolean isHas(DBObject targetNode, String key) throws Exception {
			return targetNode.containsField(key);
		}

		@Override
		public void merge(DBObject targetNode, DBObject tempNode) throws Exception {
			targetNode.putAll(tempNode);
		}

		@Override
		public void addToValueSet(BasicDBList valueSet, Object child) throws Exception {
			valueSet.add(child);
		}

		@Override
		public void addToKeyValueSet(DBObject keyValueSet, Object child, String key) throws Exception {
			keyValueSet.put(key, child);
		}

	}
}
