import React, { useEffect } from 'react';
import { Form, Input, Switch, InputNumber, Button, Card, Space, Typography, Alert } from 'antd';
import { SaveOutlined, DatabaseOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

const { Title } = Typography;

interface MySQLConfigProps {
  config: any;
  onSave: (data: any) => void;
  loading: boolean;
}

const MySQLConfig: React.FC<MySQLConfigProps> = ({ config, onSave, loading }) => {
  const { t } = useTranslation();
  const [form] = Form.useForm();

  useEffect(() => {
    if (config) {
      form.setFieldsValue({
        enable: config.enable ?? false,
        host: config.host ?? 'localhost',
        port: config.port ?? 3306,
        database: config.database ?? 'luagin',
        username: config.username ?? 'root',
        password: config.password ?? '',
        poolSize: config['pool-size'] ?? 10,
      });
    }
  }, [config, form]);

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      // 转换字段名以匹配后端期望的格式
      const formattedValues = {
        enable: values.enable,
        host: values.host,
        port: values.port,
        database: values.database,
        username: values.username,
        password: values.password,
        'pool-size': values.poolSize,
      };
      onSave(formattedValues);
    } catch (error) {
      console.error('Validation failed:', error);
    }
  };

  const cardStyle = {
    background: '#fff',
    borderRadius: 8,
    padding: 24,
    boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
    border: '1px solid #e5e6eb',
    marginBottom: 16,
  };

  return (
    <div>
      <Alert
        message={t('config.mysqlConfig.description')}
        description={t('config.mysqlConfig.descriptionDetail')}
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Form
        form={form}
        layout="vertical"
        initialValues={{
          enable: false,
          host: 'localhost',
          port: 3306,
          database: 'luagin',
          username: 'root',
          password: '',
          poolSize: 10,
        }}
      >
        <Card style={cardStyle}>
          <Title level={4}>
            <DatabaseOutlined style={{ marginRight: 8 }} />
            {t('config.mysqlConfig.basic')}
          </Title>
          
          <Form.Item
            name="enable"
            label={t('config.mysqlConfig.enable')}
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
        </Card>

        <Card style={cardStyle}>
          <Title level={4}>{t('config.mysqlConfig.connection')}</Title>
          
          <Form.Item
            name="host"
            label={t('config.mysqlConfig.host')}
            rules={[
              { required: true, message: t('config.mysqlConfig.hostRequired') },
            ]}
          >
            <Input placeholder={t('config.mysqlConfig.hostPlaceholder')} />
          </Form.Item>

          <Form.Item
            name="port"
            label={t('config.mysqlConfig.port')}
            rules={[
              { required: true, message: t('config.mysqlConfig.portRequired') },
              { type: 'number', min: 1, max: 65535, message: t('config.mysqlConfig.portRange') },
            ]}
          >
            <InputNumber
              placeholder={t('config.mysqlConfig.portPlaceholder')}
              style={{ width: '100%' }}
              min={1}
              max={65535}
            />
          </Form.Item>

          <Form.Item
            name="database"
            label={t('config.mysqlConfig.database')}
            rules={[
              { required: true, message: t('config.mysqlConfig.databaseRequired') },
            ]}
          >
            <Input placeholder={t('config.mysqlConfig.databasePlaceholder')} />
          </Form.Item>
        </Card>

        <Card style={cardStyle}>
          <Title level={4}>{t('config.mysqlConfig.credentials')}</Title>
          
          <Form.Item
            name="username"
            label={t('config.mysqlConfig.username')}
            rules={[
              { required: true, message: t('config.mysqlConfig.usernameRequired') },
            ]}
          >
            <Input placeholder={t('config.mysqlConfig.usernamePlaceholder')} />
          </Form.Item>

          <Form.Item
            name="password"
            label={t('config.mysqlConfig.password')}
            rules={[
              { required: true, message: t('config.mysqlConfig.passwordRequired') },
            ]}
          >
            <Input.Password
              placeholder={t('config.mysqlConfig.passwordPlaceholder')}
              iconRender={(visible) =>
                visible ? <EyeOutlined /> : <EyeInvisibleOutlined />
              }
            />
          </Form.Item>
        </Card>

        <Card style={cardStyle}>
          <Title level={4}>{t('config.mysqlConfig.advanced')}</Title>
          
          <Form.Item
            name="poolSize"
            label={t('config.mysqlConfig.poolSize')}
            rules={[
              { required: true, message: t('config.mysqlConfig.poolSizeRequired') },
              { type: 'number', min: 1, max: 100, message: t('config.mysqlConfig.poolSizeRange') },
            ]}
            extra={t('config.mysqlConfig.poolSizeHelp')}
          >
            <InputNumber
              placeholder={t('config.mysqlConfig.poolSizePlaceholder')}
              style={{ width: '100%' }}
              min={1}
              max={100}
            />
          </Form.Item>
        </Card>

        <div style={{ textAlign: 'center', marginTop: 24 }}>
          <Space>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={handleSave}
              loading={loading}
              size="large"
            >
              {t('config.save')}
            </Button>
          </Space>
        </div>
      </Form>
    </div>
  );
};

export default MySQLConfig; 